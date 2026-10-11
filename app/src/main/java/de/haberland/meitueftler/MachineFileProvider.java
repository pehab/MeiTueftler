package de.haberland.meitueftler;

import androidx.core.content.FileProvider;

/** Share only the dedicated export directory; never expose saves or diagnostic files. */
public final class MachineFileProvider extends FileProvider {
    public MachineFileProvider(){super();}
}
