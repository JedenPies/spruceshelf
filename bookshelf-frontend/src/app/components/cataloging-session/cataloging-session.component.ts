import { Component, computed, HostListener, inject, input, signal } from '@angular/core';
import {
  EditDraftBookCommandDto,
  ExportCompleteSseEvent,
  DraftBookCreatedSseEvent,
  DraftBookDto,
  DraftBookDeletedSseEvent,
  DraftBookUpdatedSseEvent,
} from '../../models/backend.model';
import { BackendService } from '../../services/backend.service';
import { ToastService } from '../../services/toast.service';
import { CatalogingSessionHeaderComponent } from './header/cataloging-session-header.component';
import { ExportService } from '../../services/export.service';
import { ManualIsbnModalComponent } from './manual-isbn-modal/manual-isbn-modal.component';
import { ExportModalComponent } from './export-modal/export-modal.component';
import { DraftBooksTableComponent } from './draft-books-table/draft-books-table.component';
import { EditDraftBookFormDialogComponent } from './edit-draft-book-modal/edit-draft-book-form-dialog.component';
import { QrCodeModalComponent } from '../qr-code-modal/qr-code-modal.component';
import { RecentSessionsService } from '../../services/recent-session.service';

@Component({
  selector: 'app-scanner',
  imports: [
    CatalogingSessionHeaderComponent,
    DraftBooksTableComponent,
    ManualIsbnModalComponent,
    ExportModalComponent,
    EditDraftBookFormDialogComponent,
    QrCodeModalComponent,
  ],
  templateUrl: './cataloging-session.component.html',
  styleUrl: './cataloging-session.component.scss',
  standalone: true,
})
export class CatalogingSessionComponent {
  private readonly DELETE_TOAST_ID = 'batch-delete-toast';

  exportService = inject(ExportService);
  backendService = inject(BackendService);
  toastService = inject(ToastService);
  recentSessionsService = inject(RecentSessionsService);

  isModalOpen = computed<boolean>(() => {
    return (
      this.isManualIsbnModalOpen() || this.isExportModalOpen() || this.editingDraftBook() !== null
    );
  });
  draftBooksToShow = computed<DraftBookDto[]>(() => {
    const deleIds = new Set(this.pendingDeletionDraftBooks().map((item) => item));
    return this.draftBooks().filter((draftBook) => !deleIds.has(draftBook));
  });

  sessionId = input.required<string>();

  currentShareCode = signal<string>('');

  isExportModalOpen = signal<boolean>(false);
  isManualIsbnModalOpen = signal<boolean>(false);

  draftBooks = signal<DraftBookDto[]>([]);
  isLoadingDraftBooks = signal<boolean>(true);
  pendingDeletionDraftBooks = signal<DraftBookDto[]>([]);
  editingDraftBook = signal<DraftBookDto | null>(null);

  scannerUrl = signal<string>('');
  isAttachScannerModalOpen = signal<boolean>(false);

  private deleteTimeoutId?: any;

  private eventSource?: EventSource;

  ngOnInit() {
    this.loadDraftBooks();
    this.exportService.loadExport(this.sessionId());
    this.initSseStream();
    this.scannerUrl.set(
      window.location.protocol + '//' + window.location.host + '/cataloging-session/' + this.sessionId() + '/scanner',
    );
    this.generateShareCode();
    this.recentSessionsService.addSession(this.sessionId(), this.draftBooks().length);
  }

  ngOnDestroy() {
    if (this.eventSource) {
      this.eventSource.close();
    }
  }

  handleUpdateDraftBook(command: EditDraftBookCommandDto) {
    const draftBook = this.editingDraftBook();
    if (!draftBook) return;

    this.backendService.modifyDraftBook(this.sessionId(), draftBook.id, command).subscribe({
      next: (updatedDraftBook) => {
        this.draftBooks.update((current) =>
          current.map((s) => (s.id === updatedDraftBook.id ? updatedDraftBook : s)),
        );
        this.toastService.show('Book details updated', 'success');
        this.editingDraftBook.set(null);
      },
      error: () => this.toastService.show('Failed to update book', 'error'),
    });
  }

  @HostListener('window:keydown.control.space', ['$event'])
  handleCtrlSpace(event: Event) {
    if (this.isModalOpen()) return;
    event.preventDefault(); // Zapobiega domyślnemu scrollowaniu strony spacją
    this.openManualIsbnModal();
  }

  openManualIsbnModal() {
    this.isManualIsbnModalOpen.set(true);
  }

  openAttachScanerModal() {
    this.isAttachScannerModalOpen.set(true);
  }

  closeAttachScannerModal() {
    this.isAttachScannerModalOpen.set(false);
  }

  addManualIsbn(isbn: string) {
    this.backendService.addDraftBook(this.sessionId(), isbn).subscribe({
      error: () => this.toastService.show('Błąd dodawania ISBN', 'error'),
    });
  }

  retryFetch(draftBookId: string) {
    const sessionId = this.sessionId();
    if (!sessionId) return;
    this.backendService.retryFetch(sessionId, draftBookId).subscribe({
      error: (err) => console.error('Błąd podczas ponawiania skanowania:', err),
    });
  }

  private loadDraftBooks() {
    const sessionId = this.sessionId();
    if (sessionId) {
      this.isLoadingDraftBooks.set(true);
      this.backendService.retrieveAllDraftBooks(sessionId).subscribe({
        next: (result) => {
          const sortedDraftBooks = result.sort(
            (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
          );
          this.draftBooks.set(sortedDraftBooks);
          const draftBooksLength = this.draftBooks().length;
          if (draftBooksLength === 0) {
            this.openAttachScanerModal();
          }
          this.recentSessionsService.updateSession(sessionId, draftBooksLength);
          this.isLoadingDraftBooks.set(false);
        },
        error: () => {
          this.isLoadingDraftBooks.set(false);
        }
      });
    }
  }

  private initSseStream() {
    const sessionId = this.sessionId();
    if (!sessionId) return;
    this.eventSource = new EventSource(`/api/cataloging-sessions/${sessionId}/events-stream`);
    this.eventSource.addEventListener('DRAFT_BOOK_CREATED', (event: MessageEvent) => {
      const eventDto: DraftBookCreatedSseEvent = JSON.parse(event.data);
      this.draftBooks.update((currentDraftBooks) => {
        const updated = [eventDto.draftBook, ...currentDraftBooks];
        this.recentSessionsService.updateSession(this.sessionId(), updated.length);
        return updated;
      });
      this.exportService.invalidateExport();
    });
    this.eventSource.addEventListener('DRAFT_BOOK_UPDATED', (event: MessageEvent) => {
      const eventDto: DraftBookUpdatedSseEvent = JSON.parse(event.data);
      this.draftBooks.update((currentDraftBooks) =>
        currentDraftBooks.map((draftBook) =>
          draftBook.id === eventDto.draftBook.id ? eventDto.draftBook : draftBook,
        ),
      );
      this.exportService.invalidateExport();
    });
    this.eventSource.addEventListener('DRAFT_BOOKS_DELETED', (event: MessageEvent) => {
      const eventDto: DraftBookDeletedSseEvent = JSON.parse(event.data);
      this.exportService.invalidateExport();
      this.toastService.show(`${eventDto.count} draft books permanently deleted`, 'info');
      this.recentSessionsService.updateSession(this.sessionId(), this.draftBooks().length);
    });
    this.eventSource.addEventListener('EXPORT_COMPLETE', (event: MessageEvent) => {
      const eventDto: ExportCompleteSseEvent = JSON.parse(event.data);
      this.exportService.handleSseComplete(eventDto.export);
    });
    this.eventSource.onerror = (error) => {
      console.error('EventSource failed:', error);
    };
  }

  deleteDraftBook(draftBook: DraftBookDto) {
    this.pendingDeletionDraftBooks.update((list) => [...list, draftBook]);
    if (this.deleteTimeoutId) {
      clearTimeout(this.deleteTimeoutId);
    }

    const count = this.pendingDeletionDraftBooks().length;
    const message = count === 1 ? '1 draftBook deleted' : `${count} elements deleted`;
    this.toastService.show(
      message,
      'warning',
      {
        label: 'Cancel',
        run: () => this.cancelBatchDelete(),
      },
      this.DELETE_TOAST_ID,
      10000,
    );

    this.deleteTimeoutId = setTimeout(() => {
      this.commitBatchDelete();
    }, 5000);
  }

  cancelBatchDelete() {
    if (this.deleteTimeoutId) {
      clearTimeout(this.deleteTimeoutId);
      this.deleteTimeoutId = undefined;
    }
    this.pendingDeletionDraftBooks.set([]);
    this.toastService.remove(this.DELETE_TOAST_ID);
  }

  generateShareCode() {
    this.backendService.generateShareCode(this.sessionId()).subscribe({
      next: (result) => {
        this.currentShareCode.set(result.code);
        setTimeout(() => this.generateShareCode(), 300000);
      },
    });
  }

  private commitBatchDelete() {
    const draftBooksToDelete = this.pendingDeletionDraftBooks();
    if (draftBooksToDelete.length === 0) return;
    this.toastService.remove(this.DELETE_TOAST_ID);
    this.pendingDeletionDraftBooks.set([]);
    const ids = draftBooksToDelete.map((s) => s.id);
    this.draftBooks.update((current) => current.filter((s) => !ids.includes(s.id)));
    this.backendService.deleteDraftBooks(this.sessionId(), draftBooksToDelete).subscribe({});
  }
}
