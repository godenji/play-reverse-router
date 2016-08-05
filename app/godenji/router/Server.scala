package godenji.router

trait Server { self: ReverseRouter#RoutesServer =>
	
	def routerContent(): String = {
		val packageRoutes =
			routes.map(_.call).map{c=> (
				cleanPackageName(c), 
				s"val ${c.controller} = $prefixPackageRoot${c.packageName}.routes.${c.controller}"
			)}.
			groupBy(_._1). // group on package name
			map{case(pkg, xs) => (
				pkg,
				xs.map(_._2).distinct.sortBy(identity) // ensure unique router 
			)}
		val packages =
			packageRoutes.map{case(pkg, routes) =>
				val padRoute = padSpaces(
					if(pkg == "") { // root package
						if(isLibraryRouter) 4 else 2
					}
					else if(isLibraryRouter) 6
					else 4
				)
				val routesString = 
					routes.map(route => s"${padRoute}$route").mkString("\n")
				
				if(pkg == "") s"$routesString\n"
				else {
					val pad = padRoute.dropRight(2)
					s"""
					|${pad}object $pkg {
					|$routesString
					|${pad}}
					|""".stripMargin
				}
			}.mkString
			
		val(routesString, withLibraryRouter) = 
			if(isLibraryRouter) (wrapLibraryRoutes(packages), "")
			else (
				packages, 
				s" extends ${libraryPackage}.controller.${routerClassName}"
			)
			
		s"""package $routerPackage\n
		|object $routerClassName extends $routerClassName
		|trait $routerClassName $withLibraryRouter {$routesString} 
		|""".stripMargin
	}
	
	/** 
	 *	library router is wrapped in outer `object libname {...}` namespace;
	 *	`_root_` prefix is needed for controller path in case
	 *  libname and package name are the same
	*/
	private def prefixPackageRoot: String = if(isLibraryRouter) "_root_." else ""
	
	private def padSpaces(n: Int): String = 
		(1 to n).map(_ => " ").mkString
}
