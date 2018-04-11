package controllers

import javax.inject._
import play.api._
import play.api.mvc._

// Importes necesarios para que nuestra clase funcione
import play.api.libs.json._
import models.Pet
import play.api.db._ // Este es especialmente necesario para conectarse con la BD

// Controlador de la pagina web
// NOTA: No olvidar poner >>> db: Database como parametro de la clase. OJO!
@Singleton
class HomeController @Inject()(db: Database, cc: ControllerComponents) extends AbstractController(cc) {
  
  // Se crea una lista con algunos elementos de prueba
  var mascotasLista = List[Pet](
    Pet(1, "Neron", "Bulldog", "M", "Calle 48 No.48-12", "P"),
    Pet(2, "Bruno", "Beagle", "H", "Carrera 4 No.65-75", "E")
  )

  // Metodo para recuperar todos los elementos de la lista mascotas
  def getMascotas = Action {
    val jsonAux = Json.toJson(mascotasLista) // Simplemente se toma la lista, se Jsifica
    Ok(jsonAux) // Y se retorna en la lista Jsificada
  }

  // Metodo para eliminar la mascota indicada de la lista 
  def removerMascota(id: Int) = Action {
    // Simplemente se usa el metodo find de la coleccion de mascotas y con el match
    // se busca en cual caso cae el resultado. Si encontro ALGO entonces que borre la mascota de la lista y retorne un mensaje de ello
    // O sino (cualquier otro caso) entonces se retorna un mensaje de que no se encontro la mascota a borrar.
    mascotasLista.find(_.id == id) match {
      case Some(m) => mascotasLista = mascotasLista.filter(x => x.id != id)
                      Ok("La mascota ha sido eliminada exitosamente!")
      case _ => Ok("La mascota indicada no existe o ya fue eliminada!")
    }    
  }

  // Metodo para actualizar la informacion de la mascota indicada en la lista
  def actualizarMascota = Action { implicit request =>
    val cuerpoJson = request.body.asJson.get // En primer lugar se recupera el cuerpo del mensaje el cual debe contener el json con la informacion a actualizar

    // Luego, se valida que lo que obtuve si es un json que corresponda con un objeto tipo Pet
    cuerpoJson.validate[Pet] match {
      // En caso de exito entonces
      case success: JsSuccess[Pet] =>

        // Creo un nuevo objeto mascota a partir de la informacion que me llego
        var nuevaMascota = Pet(success.get.id, success.get.name, success.get.kind, success.get.gender, success.get.location, success.get.state)

        // Ahora, se busca que la mascota a actualizar si exista, por lo que...
        mascotasLista.find(_.id == success.get.id) match {
          // Si de verdad existe entonces intercambio la mascota que acabo de crear con la mascota que tiene el mismo id
          case Some(m) => mascotasLista = mascotasLista.map(aux => if (aux.id == success.get.id) nuevaMascota else aux)
                          Ok("La mascota ha sido actualizada exitosamente!")
          // Sino entonces retorno un mensaje al respecto
          case _ => Ok("No se puede actualizar una mascota que no existe!")
        }
      // En caso de error entonces devuelvo un mensajito
      case e:JsError => BadRequest("No se pudo actualizar porque hay malos parametros!!")
    }
  }

  // Metodo para agregar una nueva mascota en la lista
  def insertarMascota = Action { implicit request =>
    val cuerpoJson = request.body.asJson.get // En primer lugar se recupera el cuerpo del mensaje el cual debe contener el json con la informacion de la mascota a ingresar a la lista

    // Luego, se valida que lo que obtuve si es un json que corresponda con un objeto tipo Pet
    cuerpoJson.validate[Pet] match {
      // En caso de exito entonces
      case success: JsSuccess[Pet] =>

        // Creo un nuevo objeto mascota a partir de la informacion que me llego
        var nuevaMascota = Pet(success.get.id, success.get.name, success.get.kind, success.get.gender, success.get.location, success.get.state)

        // Ahora, se busca que la mascota a insertar no repita id, por lo que...
        mascotasLista.find(_.id == success.get.id) match {
          // Si ya hay una mascota con tal id entonces se muestra un mensaje al respecto
          case Some(m) => Ok("No se puede insertar la mascota porque la clave indicada ya existe")
          // sino entonces se inserta la mascota y se muestra un mensaje de exito
          case _ =>  mascotasLista = mascotasLista :+ nuevaMascota
                     Ok("La mascota ha sido ingresada exitosamente!")
        }

      // En caso de error entonces devuelvo un mensajito
      case e:JsError => BadRequest("No se pudo actualizar porque hay malos parametros!!")
    }
  }
  
  // ---------------------------------------------------------------------
  
  
  // Se crea una lista vacia para manejar los datos que llegan de la BD
  var mascotas = List[Pet]()
  
  // Metodo para recuperar todos las mascotas de la BD
  def getMascotasSQL = Action {
    // En primer lugar creamos una variable para realizar la conexion con la BD
    val conexion = db.getConnection()
    
    // A continuación inicializamos (vaciamos) la lista con la que procesaremos los datos que lleguen de la BD
    mascotas = List[Pet]()
    
    try{
      // Ahora creamos una variable en donde formularemos nuestra query SQL de busqueda y la ejecutamos
      val query = conexion.createStatement
      val resultado = query.executeQuery("SELECT * FROM pet")
      
      // Ya con el resultado de la consulta, creamos objetos mascota y los agregamos a la lista de apoyo
      while (resultado.next()) {
        var p = Pet(resultado.getInt("id"),resultado.getString("name"),resultado.getString("kind"),resultado.getString("gender"),resultado.getString("location"),resultado.getString("state"))
        mascotas = mascotas :+ p
      }
    }
    finally{
      // Antes de retornar los resultados, cerramos la conexion a la BD
      conexion.close()
    }
    
    val jsonAux = Json.toJson(mascotas) // Finalmente, se Jsifican los resultados
    Ok(jsonAux) // Y se retorna la lista de mascotas Jsificada
  }
  
  // Método para eliminar la mascota indicada de la BD
  def removerMascotaSQL(id: Int) = Action {
    // En primer lugar creamos una variable para realizar la conexion con la BD
    val conexion = db.getConnection()
    
    try{
      // Despues creamos una variable en donde formularemos nuestra query SQL de eliminacion y la ejecutamos
      val query = conexion.createStatement
      val resultado = query.executeUpdate("DELETE FROM pet WHERE id = " + id)
    }
    finally{
      // Antes de terminar, cerramos la conexion a la BD
      conexion.close()
    }
    
    // Y para terminar indicamos que se realizo la operacion correctamente
    Ok("La mascota ha sido eliminada exitosamente!")
  }

  // Metodo para actualizar la información de la mascota indicada en la BD
  def actualizarMascotaSQL = Action { implicit request =>
    val cuerpoJson = request.body.asJson.get // En primer lugar se recupera el cuerpo del mensaje el cual debe contener el json con la informacion a actualizar
    
    // Luego, se valida que lo que se obtuve si es un json que corresponda con un objeto tipo Pet
    cuerpoJson.validate[Pet] match {
      // En caso de exito entonces
      case success: JsSuccess[Pet] =>
        // Se crea un nuevo objeto mascota a partir de la informacion que me llego
        var nuevaMascota = Pet(success.get.id, success.get.name, success.get.kind, success.get.gender, success.get.location, success.get.state)
        
        // Tambien, creamos una variable para realizar la conexion con la BD
        val conexion = db.getConnection()
        
        try{
          // Despues creamos una variable en donde formulamos nuestra query SQL de actualizacion y la ejecutamos
          val query = conexion.createStatement
          val resultado = query.executeUpdate("UPDATE pet SET name = '"+nuevaMascota.name+"', kind = '"+nuevaMascota.kind+"', gender = '"+nuevaMascota.gender+"', location = '"+nuevaMascota.location+"',state = '"+nuevaMascota.state+"' WHERE id = "+nuevaMascota.id)
        }
        finally{
          // Posterior , cerramos la conexion a la BD
          conexion.close()
        }
        
        // Y para terminar indicamos que se realizo la operacion correctamente
        Ok("La mascota ha sido actualizada exitosamente!")
        
      // En caso de error entonces devuelvo un mensajito
      case e:JsError => BadRequest("No se pudo actualizar porque hay malos parametros!!")
    }
  }
  
  // Método para agregar una nueva mascota en la BD
  def insertarMascotaSQL = Action { implicit request =>
    val cuerpoJson = request.body.asJson.get // En primer lugar se recupera el cuerpo del mensaje el cual debe contener el json con la informacion de la mascota a ingresar a la lista

    // Luego, se valida que lo que se obtuvo si es un json que corresponda con un objeto tipo Pet
    cuerpoJson.validate[Pet] match {
      // En caso de exito entonces
      case success: JsSuccess[Pet] =>
        // Creo un nuevo objeto mascota a partir de la informacion que me llego
        var nuevaMascota = Pet(success.get.id, success.get.name, success.get.kind, success.get.gender, success.get.location, success.get.state)
        
        // Tambien, creamos una variable para realizar la conexion con la BD
        val conexion = db.getConnection()
        
        try{
          // Despues creamos una variable en donde formulamos nuestra query SQL de inserccion y la ejecutamos
          val query = conexion.createStatement
          val resultado = query.executeUpdate("INSERT INTO pet VALUES("+nuevaMascota.id+",'"+nuevaMascota.name+"','"+nuevaMascota.kind+"','"+nuevaMascota.gender+"','"+nuevaMascota.location+"','"+nuevaMascota.state+"')")
          
          // Si todo es correcto mostramos un mensaje de exito
          Ok("La mascota ha sido ingresada exitosamente!")
        }
        catch
        {
          // En caso que pase algo mostramos un mensaje de error
          case e: Exception => Ok("No se puede insertar la mascota porque la clave indicada ya existe o hubo un error.")
        }
        finally{
          // Finalmente, cerramos la conexion a la BD
          conexion.close()
        }
        
      // En caso de error entonces devuelvo un mensajito
      case e:JsError => BadRequest("No se pudo actualizar porque hay malos parametros!!")
    }
  }
  
  // Metodo para invocar la pagina de inicio de nuestro sitio web
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
}
