package godenji.router

import play.routes.compiler._

import play.api.mvc.{Controller => _Controller}
import play.api.{Configuration, Environment}
import java.io.{File, PrintWriter}

/** utility for generating server-side and client-side reverse routers */
trait ReverseRouter { self =>
	
	/** fully qualified root package name (e.g. com.company) */
	def packageName: String
	
	/** routerPackage of library (used to mixin generated library router trait) */
	def libraryPackage: String
	
	private def systemPath = System.getProperty("user.dir")
	def routerClassName = "Router"
	def routerFileName = s"${routerClassName}.scala"
	def routerPackage =  s"${packageName}.controller"
	
	/** optional list of route paths to exclude from generated router */
	def routeExcludes: List[String] = List.empty
	
	/** 
	 *  optional list of route files to exclude from generated router 
	 *	(needed when primary router depends on a library router)
	 */
	def routeFileExcludes: List[String] = List.empty
	
	trait RouterContract {
		
		/** relative path of project working directory (e.g. /module/foo/app/) */
		def projectPath: String
		
		/** fully qualified package name of router (e.g. com.company.controller) */
		def routerPackage: String = self.routerPackage
		def libraryPackage: String = self.libraryPackage
		
		/** route generator entry point */
		def routes2File(): Either[String, String]
		
		/** absolute path to route target directory */
		def routerPath = (
			s"$systemPath$projectPath${routerPackage.split("\\.").mkString("/")}/"
		)
		/** router file handle */
		def routerFile = new File(s"$routerPath$routerFileName")
		
		/** routes to exclude from generated router */
		def routeExcludes = 
			List("/routegen") ++ self.routeExcludes
	}
	
	/** generate a server-side reverse router */
	abstract class RoutesServer extends RouteMixin with Server {
		val forBrowser = false
	}
	
	/** generate a client-side reverse router */
	abstract class RoutesBrowser(
		val siteDomain: String,
		val _routeImports: List[String]
	) 
	extends RouteMixin with Browser {
	  val forBrowser = true
	}
	
	private[router] trait RouteMixin
		extends _Controller 
		with RoutesHelper 
		with RouterContract {
		
		def env: Environment
		def config: Configuration
		
		def routeIncludes: List[String] = List.empty
  	def routerContent(): String
  	def forBrowser: Boolean
		
  	val routeFileExcludes: List[String] = self.routeFileExcludes
		val routerClassName = self.routerClassName
  	lazy val routes = collectRoutes
  	
  	def routes2File() = {
			restrict(env)
	    writeRoutes(routerContent(), forBrowser)
		}
		
		private def restrict(env: Environment): Unit = 
			require(
				env.mode == play.api.Mode.Dev,
				"reverse router can only be generated in Dev mode!"
			)
			
		private def writeRoutes(content: String, forBrowser: Boolean) = {
			val res = print2File( routerFile )(_.println(content))
	    res.right.map{unit =>
	    	val which = if(forBrowser) "client-side" else "server-side"
	    	s"$which reverse router $routerFileName written to $routerPath"
			}
		}
		
		private 
			def print2File(f: File)(op: PrintWriter => Unit): Either[String, Unit] = {
				val p = new PrintWriter(f)
				try { Right(op(p)) } 
				catch {case e: Exception => Left(e.getMessage)} 
				finally { p.close() }
			}
	}
		
}