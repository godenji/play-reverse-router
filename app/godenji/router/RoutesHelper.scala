package godenji.router

import play.routes.compiler._
import play.api.{Configuration, Environment}
import java.io.File
import scala.io.Source

trait RoutesHelper {
	def env: Environment
	def config: Configuration
	def routes: List[Route]
		
	/** route paths to include in generated router (empty = include all) */
	def routeIncludes: List[String]
	
	/** route paths to exclude from generated router */
	def routeExcludes: List[String]

  def collectRoutes: List[Route] = {	
    def apply(routeFile: String, prefix: String): List[Route] = {
    	val data =  
    		Source.fromInputStream(
      		env.classLoader.getResourceAsStream(routeFile)
      	).mkString
      	
      val parsed = RoutesFileParser.parseContent(data, new File(routeFile))
      parsed.right.toSeq.flatten.toList.collect {
        case(r: Route) =>
          List(r.copy(
          	path = r.path.copy(Seq(StaticPart(prefix)) ++ r.path.parts)
          ))
        case(inc: Include) => 
        	apply(
        		inc.router.toLowerCase, inc.prefix
        	)
      }.flatten
    }
    filterRoutes(apply(routeFile, ""))
  }
	
	private lazy val routeFile = (
		if(!config.underlying.hasPath("play.http.router")) "routes" 
		else 
			config.getString("play.http.router").map(_.toLowerCase).
			getOrElse("routes")
  )
  
	/** restrict routes to routeIncludes */
	private def filterRoutes(routes: List[Route]): List[Route] = {
		routes.filter{x=>
			val uri = 
				x.path.parts.collect{
					case(part: StaticPart) => part.value
				}.filterNot(_ == "").mkString("/")
				
			includesFn(uri) && !excludesFn(uri)
		}
	}
	private lazy val (includesFn, excludesFn) = (
		(
			if(routeIncludes.isEmpty) (uri: String) => true // i.e. include all
			else (uri: String) => routeIncludes.exists(uri.startsWith) 
		),
		(uri: String) => routeExcludes.exists(uri.startsWith)		
	)
	
	def cleanPackageName(c: HandlerCall): String = {
		val parts = c.packageName.split("\\.").tail // omit "controllers."
		if(parts.isEmpty) "root" // root package
		else parts.mkString("_") // convert multi-package foo.bar to foo_bar
	}
			
}