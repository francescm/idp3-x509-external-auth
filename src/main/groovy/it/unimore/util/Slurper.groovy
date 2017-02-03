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
        def value = Eval.x(slurper, "x.${key}")
        return value
    }
}
