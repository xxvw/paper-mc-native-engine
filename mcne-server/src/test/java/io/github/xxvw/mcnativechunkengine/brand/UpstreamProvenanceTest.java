package io.github.xxvw.mcnativechunkengine.brand;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpstreamProvenanceTest {
    @Test
    void embeddedBaselineRetainsTruthfulIdentity() {
        UpstreamProvenance provenance = UpstreamProvenance.load();
        assertEquals("paper-chunk-native-engine", UpstreamProvenance.BRAND);
        assertEquals("26.3", provenance.minecraftVersion());
        assertEquals("181", provenance.paperBuild());
        assertEquals("cc7ee9aebdfc9e4acc3ef8f38c35944872fd1f87", provenance.paperCommit());
        assertTrue(provenance.display().startsWith("Upstream: Paper 26.3 build 181 @ "));
    }
}
