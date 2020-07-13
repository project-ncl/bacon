<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>

<html>
<head>
    <meta charset="utf-8">
    <title>${pigConfiguration.product.name} ${pigConfiguration.version}</title>
</head>
<body>
<h1>${pigConfiguration.product.name} ${pigConfiguration.version}</h1>

<h2>Product Deliverables</h2>
<ul>
    <li><a href="${deliverables.repositoryZipName}">${deliverables.repositoryZipName}</a> - Maven repository</li>
    <li><a href="${deliverables.sourceZipName}">${deliverables.sourceZipName}</a> - Product source</li>
    <li><a href="${deliverables.licenseZipName}">${deliverables.licenseZipName}</a> - Product licenses</li>
    <#if deliverables.javadocZipName??>
        <li><a href="${deliverables.javadocZipName}">${deliverables.javadocZipName}</a> - Product javadoc</li>
    </#if>
    <li><a href="${deliverables.nvrListName}">${deliverables.nvrListName}</a> - Product NVR list</li>
</ul>
<h2>Extras and Information - Non-Releasable</h2>
<ul>
    <li><a href="extras/${deliverables.sharedContentReport}">${deliverables.sharedContentReport}</a> - Shared Content report based on NCL/Brew build</li>
    <li><a href="extras/${deliverables.communityDependencies}">${deliverables.communityDependencies}</a> - Community dependencies that were found</li>
    <li><a href="extras/${deliverables.repoCoordinatesName}">${deliverables.repoCoordinatesName}</a> - NCL/Brew coordinates of project builds making up this release</li>
    <li><a href="extras/${deliverables.artifactListName}">${deliverables.artifactListName}</a> - List of all runtime artifacts in the Maven repository</li>
    <li><a href="extras/${deliverables.duplicateArtifactListName}">${deliverables.duplicateArtifactListName}</a> - List of artifacts present in the Maven repository more than once</li>
</ul>

<h2>Builds performed:</h2>
<table>
    <thead>
    <tr>
        <th>Name</th>
        <th>URL</th>
        <th>Internal SCM URL (add git+ssh://[user]@ prefix to clone it)</th>
        <th>SCM revision</th>
    </tr>
    </thead>
    <tbody>
    <#list builds as build>
    <tr>
        <td>${build.name}</td>
        <td>
            <a href="http://${pncUrl}/pnc-web/#/build-records/${build.id}">Build
                #${build.id}</a>
        </td>
        <td>${build.scmUrl}</td>
        <td>${build.scmRevision}</td>
    </tr>
    </#list>
    </tbody>
</table>
</body>
</html>
