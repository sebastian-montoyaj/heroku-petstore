package models

// Importes necesarios para hacer el modelo Pet
import com.sun.org.apache.xpath.internal.operations.Or
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.functional.syntax._

// Se crea la clase Pet, en la cual los parametros deben coincidir con los campos de la tabla Pet de la base de datos
case class Pet(id: Int, name: String, kind: String, gender: String, location: String, state: String)

// También se crea un objeto Pet con el fin de implementar los metodos para escribir Pet como si fueran Jsons
object Pet {

  implicit val petWrite = Json.writes[Pet]
  implicit val petRead = Json.reads[Pet]

}
