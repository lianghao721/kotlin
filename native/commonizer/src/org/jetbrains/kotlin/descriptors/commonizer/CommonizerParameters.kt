/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsCollector

class CommonizerParameters(
    val statsCollector: StatsCollector? = null,
    val progressLogger: ((String) -> Unit)? = null
) {
    // use linked hash map to preserve order
    private val _targetProviders = LinkedHashMap<LeafTarget, TargetProvider>()
    val targetProviders: List<TargetProvider> get() = _targetProviders.values.toList()
    val sharedTarget: SharedTarget get() = SharedTarget(_targetProviders.keys)

    // common module dependencies (ex: Kotlin stdlib)
    var dependeeModulesProvider: ModulesProvider? = null
        set(value) {
            check(field == null)
            field = value
        }

    fun addTarget(targetProvider: TargetProvider): CommonizerParameters {
        require(targetProvider.target !in _targetProviders) { "Target ${targetProvider.target} is already added" }
        _targetProviders[targetProvider.target] = targetProvider

        return this
    }

    private var _resultsConsumer: ResultsConsumer? = null
    var resultsConsumer: ResultsConsumer
        get() = _resultsConsumer ?: error("Results consumer has not been set")
        set(value) {
            check(_resultsConsumer == null)
            _resultsConsumer = value
        }

    fun hasAnythingToCommonize(): Boolean {
        if (_targetProviders.size < 2) return false // too few targets

        val allModuleNames: List<Set<String>> = _targetProviders.values.map { targetProvider ->
            targetProvider.modulesProvider.loadModuleInfos().keys
        }
        val commonModuleNames: Set<String> = allModuleNames.reduce { a, b -> a intersect b }

        return commonModuleNames.isNotEmpty() // there are modules that are present in every target
    }
}
