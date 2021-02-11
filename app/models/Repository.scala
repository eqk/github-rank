package models

import play.api.libs.json._

case class Repository(name: String)

trait RepositoryJson {
  implicit val format: Format[Repository] = Json.format[Repository]
}

object Repository extends RepositoryJson
