package org.ostelco.prime.admin.importer

import arrow.core.Either
import org.ostelco.prime.core.ApiError
import org.ostelco.prime.core.ApiErrorCode
import org.ostelco.prime.core.BadRequestError
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource

interface ImportProcessor {
    fun import(importDeclaration: ImportDeclaration): Either<ApiError, Unit>
}

class ImportAdapter : ImportProcessor {

    private val adminDataStore by lazy { getResource<AdminDataSource>() }

    override fun import(importDeclaration: ImportDeclaration): Either<ApiError, Unit> {

        return adminDataStore.atomicImport(
                offer = importDeclaration.offer,
                products = importDeclaration.products,
                segments = importDeclaration.segments)
                .mapLeft { BadRequestError(it.message, ApiErrorCode.FAILED_TO_IMPORT_OFFER) }
    }
}