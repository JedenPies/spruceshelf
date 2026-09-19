package net.patrykdobrowolski.bookshelf.adapter.db;

import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import net.patrykdobrowolski.bookshelf.adapter.db.entity.ExportEntity;
import net.patrykdobrowolski.bookshelf.adapter.db.mapper.ExportEntityMapper;
import net.patrykdobrowolski.bookshelf.adapter.db.repository.SpringDataExportRepository;
import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.export.Export;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportType;
import net.patrykdobrowolski.bookshelf.domain.port.ExportRepositoryPort;

import java.util.Optional;
import java.util.UUID;

@Named
@RequiredArgsConstructor
public class ExportRepositoryAdapter implements ExportRepositoryPort  {

    private final SpringDataExportRepository exportRepository;
    private final ExportEntityMapper exportEntityMapper;

    @Override
    public Export findById(UUID id) throws ExportException.ExportNotFoundException {
        ExportEntity found = exportRepository.findById(id).orElseThrow(ExportException.ExportNotFoundException::new);
        return exportEntityMapper.fromEntity(found);
    }

    @Override
    public Optional<Export> findByTypeAndCorrelationKey(ExportType type, UUID correlationKey) {
        Optional<ExportEntity> found = exportRepository.findByTypeAndCorrelationKey(type, correlationKey);
        return found.map(exportEntityMapper::fromEntity);
    }

    @Override
    public Export save(Export export) {
        ExportEntity saved = exportRepository.save(exportEntityMapper.toEntity(export));
        return exportEntityMapper.fromEntity(saved);
    }
}
