package org.acme;

import io.quarkiverse.githubaction.Action;

class MyAction {

    @Action
    fun test() {
        println("Hello from Quarkus GitHub Action")
    }
}
