package it.unimore.util

class Slurper {

    def slurper

    Slurper(String filename) {
        File file = new File(filename)
        URI uri = null

        uri = file.toURI()

        slurper = new ConfigSlurper().parse(uri.toURL())
    }

    def fetch(key) {
        slurper[key]

        String dn = "MLVFNC69H12B819Z/7430035000001454.Caud0cp/FVmUXl/uO8quWcFGzOQ="
        def pattern = /^([^\/]+)\/.+$/

        def matcher = (dn =~ pattern)
        println matcher
        println matcher[0][1]
    }
}
