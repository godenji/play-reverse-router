package godenji.router

trait BrowserBase {
  /** domain name of application */
  def siteDomain: String

  /** custom route imports */
  def routeImports: List[String]

  private[router] def jsImports =
    s"""
${routeImports.map(x => s"import $x").mkString("\n")}
"""

  private[router] final def routerBase(): String = {

    val base = s"""
trait RouterBase extends RouterContract {
  val domain = window.location.host

  def isDev = !domain.contains(siteDomain)
  val isSecure = window.location.protocol == "https:"
  val port = window.location.port
  val httpPort = (
    if(port == "") "80"
    else if(isSecure && port != "443") port
    else "80"
  )
  val httpsPort = (
    if(port == "") "443" 
    else if(!isSecure && port != "80") port
    else "443"
  )
  def http  = domainWrap("HTTP", isDev) 
  def https = domainWrap("HTTPS", isDev)
}
"""

    s"$jsImports$contract$base"
  }

  private def contract =
    """
trait RouterContract {
  def domain: String
  def siteDomain: String

  /**
   * http link prefix
   *   e.g. http://www.domain.com + optional :port
   */
  def http: String
  /**
   * https link prefix
   *   e.g. https://www.domain.com + optional :port
   */
  def https: String

  def httpPort: String
  def httpsPort: String

  /**
   * generate http(s) link prefix as:
   *   http(s):// + www/sub + .domain + optional :(non-standard)port
   */
  protected def domainWrap
    (proto: String, isDev: Boolean): String = {
    val port = (proto match {
      case "HTTP"  if httpPort   != "80" => s":$httpPort"
      case "HTTPS" if httpsPort != "443" && !isDev => s":$httpsPort"
      case _ => ""
    })
    val protoStr = proto.toString.toLowerCase

    // no subdomain or https in local dev
    if(isDev) s"http://${domain}${port}"
    else {
      if(isSubdomain) s"$protoStr://${domain}${port}" 
      else s"$protoStr://www.${domain}${port}"
    }
  }

  protected def isSubdomain: Boolean = (
    domain.split("\\.").size > 2
  )
}
"""
}
