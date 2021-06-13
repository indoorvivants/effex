package effex.docs

import java.time.LocalDate

import subatomic._
import subatomic.builders._
import subatomic.builders.librarysite._

object Build extends LibrarySite.App {
  val base = os.pwd / "modules" / "docs"
  override def extra(site: Site[LibrarySite.Doc]) = {
    site
      .addCopyOf(SiteRoot / "CNAME", base / "assets" / "CNAME")
  }

  val currentYear = LocalDate.now().getYear()

  def config = LibrarySite(
    contentRoot = base / "pages",
    name = "Effex",
    githubUrl = Some("https://github.com/indoorvivants/effex"),
    assetsFilter = _.baseName != "CNAME",
    copyright = Some(s"© 2021-$currentYear Anton Sviridov"),
    assetsRoot = Some(os.pwd / "docs" / "assets"),
    highlightJS = HighlightJS.default.copy(theme = "monokai-sublime")
  )
}
