package models

import play.api.libs.json._

case class Contributor(
  login: String,
  contributions: Int
)

trait ContributorJson {
  implicit val reads: Reads[Contributor] = Json.reads[Contributor]

  implicit val writes: Writes[Contributor] = (c: Contributor) => Json.obj(
    "name" -> c.login,
    "contributions" -> c.contributions
  )
}

object Contributor extends ContributorJson
