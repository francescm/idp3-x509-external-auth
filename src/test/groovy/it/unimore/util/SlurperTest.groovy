import org.junit.Test
import static org.junit.Assert.assertEquals

import it.unimore.util.Slurper

class SlurperTest {

    @Test
    void testConstructor() {
        def slurper = new Slurper("src/main/resources/config.slurp")
        assertEquals(slurper.slurper.X509External.test.controlCode, "test")

    }

    @Test
    void testFetch() {
        def slurper = new Slurper("src/main/resources/config.slurp")
        assertEquals(slurper.fetch("X509External.test.controlCode"), "test")
    }

    @Test
    void testRegex() {
        def slurper = new Slurper("src/main/resources/config.slurp")
        String dn = slurper.fetch("X509External.test.dn")
        def pattern = slurper.fetch("X509External.cnTransform.regex")
        def matcher = (dn =~ pattern)
        assertEquals(matcher[0][1], "MLVFNC69H12B819Z")
    }

}
