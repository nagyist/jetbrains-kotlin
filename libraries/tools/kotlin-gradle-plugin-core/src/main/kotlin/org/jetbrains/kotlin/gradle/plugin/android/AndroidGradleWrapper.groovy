package org.jetbrains.kotlin.gradle.plugin.android

import com.android.build.gradle.BasePlugin
import org.gradle.api.tasks.util.PatternFilterable

class AndroidGradleWrapper {
    static def getRuntimeJars(BasePlugin basePlugin) {
        if (basePlugin.getMetaClass().getMetaMethod("getRuntimeJarList")) {
            return basePlugin.getRuntimeJarList()
        }
        else {
            return basePlugin.getBootClasspath()
        }
    }

    static def setJavaSrcDir(Object androidSourceSet, Object kotlinDirSet) {
      androidSourceSet.getJava().srcDir(kotlinDirSet)
    }

   static def PatternFilterable getResourceFilter(Object androidSourceSet) {
      if (androidSourceSet.getResources() != null) {
        return androidSourceSet.getResources().getFilter()
      }
      return null
   }
}
