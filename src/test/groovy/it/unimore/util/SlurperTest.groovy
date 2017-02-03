import org.junit.Test
import org.junit.Before
import static org.junit.Assert.assertEquals


import static groovy.test.GroovyAssert.shouldFail
import it.unimore.util.Slurper

class SlurperTest {

    @Before
    void setUp() {

    }

    @Test
    void testConstructor() {
        def slurper = new Slurper("src/main/resources/config.slurp")
        assertEquals(slurper.slurper.X509External.test.controlCode, "test")

    }

}
