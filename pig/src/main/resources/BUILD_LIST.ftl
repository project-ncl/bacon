[
<#list builds as build>
    {
        "id":"${build.id}",
        "name":"${build.name}",
        "url":"${pncUrl}/pnc-web/#/builds/${build.id}",
        "internalScmUrl":"${build.internalScmUrl}",
        "scmRevision":"${build.scmRevision}",
        "scmTag":"${build.scmTag}"
    }<#sep>,</#sep>
</#list>
]
