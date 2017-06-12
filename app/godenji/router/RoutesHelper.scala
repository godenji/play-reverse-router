package godenji.router

import play.routes.compiler._
import play.api.{Configuration, Environment}
import java.io.File
import scala.io.Source

trait RoutesHelper {
	def env: Environment
	def config: Configuration
	def routes: List[Route]
	def routeFileExcludes: List[String]
	
	/** 
	 *  name of the root routes file (play convention is "routes")
	 *  will likely be different for library supplied router (e.g. "libname.routes")
	 */
	def rootRoutesFile: String
	
	/** whether or not generate router will be used as a dependent router */
	def isLibraryRouter: Boolean = rootRoutesFile != "routes"
	
	/**
	 * wraps library router's routes in outer `object namespace {...}`
	 */
	def wrapLibraryRoutes(routes: String) = {
			s"""
			|  object ${rootRoutesFile.split("\\.").headOption.mkString} {
			|${routes}
			|  }
			|""".stripMargin
	}
		
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
        case(inc: Include) 
        	if !routeFileExcludes.exists(_ == inc.router.toLowerCase)  => 
        		apply(
        			inc.router.toLowerCase, inc.prefix
        		)
      }.flatten
    }
    filterRoutes(apply(routeFile, ""))
  }
	
	private lazy val routeFile = (
		if(!config.underlying.hasPath("play.http.router")) rootRoutesFile 
		else 
			config.get[String]("play.http.router").toLowerCase
  )
  
	/** restrict routes to routeIncludes */
	private def filterRoutes(routes: List[Route]): List[Route] = {
		routes.filter{x=>
			val uri = 
				x.path.parts.collect{
					case(part: StaticPart) => part.value
				}.mkString("/")
				
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
		val parts = c.packageName.split("\\.").filterNot(_ == "controllers")
			
		// library router's routes are wrapped in outer libname {...} namespace
		// so no need to prefix each package a la libname_account {...}
		if(isLibraryRouter) parts.tail.mkString("_")
			
		// else convert multi-package foo.bar to foo_bar
		else parts.mkString("_")
	}
			
}