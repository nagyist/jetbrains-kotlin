package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.impl.ColumnNameGenerator

internal class Join0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.other: PluginDataFrameSchema by dataFrame()
    val Arguments.selector: ColumnMatchApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val nameGenerator = ColumnNameGenerator()
        val left = receiver.columns()
        val right = removeImpl(other.columns(), setOf(selector.right.path.path)).updatedColumns

        val rightColumnGroups = right.filterIsInstance<SimpleColumnGroup>().associateBy { it.name }

        val mergedGroups = buildMap {
            left.filterIsInstance<SimpleColumnGroup>().forEach { leftGroup ->
                val rightGroup = rightColumnGroups[leftGroup.name]
                if (rightGroup != null) {
                    val merge = ColumnNameGenerator()
                    val columns = (leftGroup.columns() + rightGroup.columns()).map { column ->
                        val uniqueName = merge.addUnique(column.name)
                        column.rename(uniqueName)
                    }
                    put(leftGroup.name, SimpleColumnGroup(leftGroup.name, columns, anyRow))
                }
            }
        }

        fun MutableList<SimpleCol>.addColumns(columns: List<SimpleCol>) {
            for (column in columns) {
                val uniqueName = nameGenerator.addUnique(column.name)
                val colToAdd = mergedGroups[column.name] ?: column
                add(colToAdd.rename(uniqueName))
            }
        }

        val columns = buildList {
            addColumns(left)
            addColumns(right.filterNot { it is SimpleColumnGroup && it.name() in mergedGroups })
        }
        return PluginDataFrameSchema(columns)
    }
}
