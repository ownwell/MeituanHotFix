package com.cyning

import com.android.build.gradle.AppExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project

/**
 * Created by hp on 2016/4/8.
 */
public class Register implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.logger.error "================自定义插件成功！=========="
        def android = project.extensions.findByType(AppExtension)
        android.registerTransform(new PreDexTransform(project))
    }
}
