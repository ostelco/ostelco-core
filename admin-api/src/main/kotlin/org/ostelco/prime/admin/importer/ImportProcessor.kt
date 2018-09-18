package org.ostelco.prime.admin.importer

interface ImportProcessor {
    fun import(decl: ImportDeclaration) : Boolean
}

class ImportAdapter : ImportProcessor {

    override fun import(decl: ImportDeclaration): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}