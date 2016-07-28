package godenji.router

import play.routes.compiler._

trait Browser 
	extends BrowserBase { self: ReverseRouter#RoutesBrowser =>
		
	val routeImports = List("godenji.model.key._") ++ _routeImports
	
	/** "$" placeholder for runtime string interpolation */
	private val runtime = "$"
	
	def routerContent() = {
val prefix =
s"""
package $routerPackage
${routerBase()}

object $routerClassName 
  extends RouterBase { self =>

  case class Route(method: String, uri: String) {
    override def toString() = uri
    def http  = s"$runtime{self.http}${runtime}uri"
    def https = s"$runtime{self.https}${runtime}uri"
  }

  private def encode[T](param: Option[T]) = {
    encodeURI(param.map(_.toString).getOrElse(""))
  }"""

s"""
$prefix
${routerBody()}
}
"""
	}
	
	private def routerBody(): String = {
		routes.groupBy(x=> cleanPackageName(x.call)).map{case(pkg, routes) =>
			val controllers = 
				routes.groupBy(_.call.controller).map{case(ctrl, routes) =>
					val methods = parseMethods(routes.filter(_.call.controller == ctrl))
					s"""
					|    object $ctrl {
					|      ${methods.mkString("\n")}
					|    }""".stripMargin
				}
		  s"""
		  |  object $pkg {
		  |    ${controllers.mkString("\n")}
		  |  }\n""".stripMargin	  
		}.mkString
	}
	
	private def parseMethods(routes: List[Route]): List[String] = {
		routes.map{x=>
			val paths = 
				x.path.parts.collect{
					case(p: StaticPart)  => p.value
					case(p: DynamicPart) => s"$runtime{encodeURI(${p.name})}"
				}
			val params = 
				x.call.parameters.map(
					_.collect{case p if(p.typeName.startsWith("Option")) => 
						s"${p.name}=$runtime{encode(${p.name})}"
					}.mkString("&")
				).filterNot(_ == "")
				
			val uriPath   = s"/${paths.mkString("/")}".replaceAll("//", "/")
			val uriParams = params.map(x=> s"?$x").mkString
			val methodParams = 
				x.call.parameters.map{p=> 
					p.map(_.toString.replace("?=", "=")).mkString(", ")
				}.getOrElse("")
			
			val method =
				s"""
				|      def ${x.call.method}($methodParams) = Route(
				|        "${x.verb}",
				|        s"$uriPath$uriParams"
				|      )""".stripMargin
			method
		}
	}
		
}