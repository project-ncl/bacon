<#list builds as build>
${build.name}_SCM_URL=${build.internalScmUrl}
${build.name}_SCM_REVISION=${build.scmRevision}
</#list>