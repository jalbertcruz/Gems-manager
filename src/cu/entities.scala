package cu

case class GemEntry(
                     name: String,
                     last_version: String)

case class GemLocData(
                       name: String,
                       version: String)

case class GemDep(
                   name: String,
                   requirements: String)

case class GemInfo(
                    name: String,
                    downloads: Int,
                    version: String,
                    version_downloads: Int,
                    authors: String,
                    info: String,
                    project_uri: String,
                    gem_uri: String,
                    homepage_uri: String,
                    wiki_uri: String,
                    documentation_uri: String,
                    mailing_list_uri: String,
                    source_code_uri: String,
                    bug_tracker_uri: String,
                    dependencies: Map[String, List[GemDep]])

case class RunConfig(var actual_gem: Int, total: Int)
