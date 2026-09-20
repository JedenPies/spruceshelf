package net.patrykdobrowolski.bookshelf.adapter.exporter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
@Getter
public class ExportResult {

    private final byte[] data;
}
