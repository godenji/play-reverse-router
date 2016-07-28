package godenji.router

trait Server { self: ReverseRouter#RoutesServer =>
	
	def routerContent(): String = {
		val packageRoutes =
			routes.map(_.call).map{c=> (
				cleanPackageName(c), 
				s"val ${c.controller} = ${c.packageName}.routes.${c.controller}"
			)}.
			groupBy(_._1). // group on package name
			map{case(pkg, xs) => (
				pkg,
				xs.map(_._2).distinct.sortBy(identity) // ensure unique router 
			)}
		val packages =
			packageRoutes.map{case(pkg, routes) =>
				s"""
				|  object $pkg {
				|${routes.map(route => s"    $route").mkString("\n")}
				|  }
				|""".stripMargin
			}
		s"""package $routerPackage\n
		|class $routerClassName {${packages.mkString}} 
		|""".stripMargin
	}
	
}