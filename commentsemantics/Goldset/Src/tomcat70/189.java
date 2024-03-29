import java.io.IOException;
import java.io.PrintWriter;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.HTMLFilter;

public class RequestInfoExample extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final ResourceBundle RB = ResourceBundle.getBundle("LocalStrings");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        String title = RB.getString("requestinfo.title");
        out.println("<title>" + title + "</title>");
        out.println("</head>");
        out.println("<body bgcolor=\"white\">");
        // img stuff not req'd for source code html showing
        // all links relative!
        // XXX
        // making these absolute till we work out the
        // addition of a PathInfo issue
        out.println("<a href=\"../reqinfo.html\">");
        out.println("<img src=\"../images/code.gif\" height=24 " + "width=24 align=right border=0 alt=\"view code\"></a>");
        out.println("<a href=\"../index.html\">");
        out.println("<img src=\"../images/return.gif\" height=24 " + "width=24 align=right border=0 alt=\"return\"></a>");
        out.println("<h3>" + title + "</h3>");
        out.println("<table border=0><tr><td>");
        out.println(RB.getString("requestinfo.label.method"));
        out.println("</td><td>");
        out.println(HTMLFilter.filter(request.getMethod()));
        out.println("</td></tr><tr><td>");
        out.println(RB.getString("requestinfo.label.requesturi"));
        out.println("</td><td>");
        out.println(HTMLFilter.filter(request.getRequestURI()));
        out.println("</td></tr><tr><td>");
        out.println(RB.getString("requestinfo.label.protocol"));
        out.println("</td><td>");
        out.println(HTMLFilter.filter(request.getProtocol()));
        out.println("</td></tr><tr><td>");
        out.println(RB.getString("requestinfo.label.pathinfo"));
        out.println("</td><td>");
        out.println(HTMLFilter.filter(request.getPathInfo()));
        out.println("</td></tr><tr><td>");
        out.println(RB.getString("requestinfo.label.remoteaddr"));
        out.println("</td><td>");
        out.println(HTMLFilter.filter(request.getRemoteAddr()));
        out.println("</td></tr>");
        String cipherSuite = (String) request.getAttribute("javax.servlet.request.cipher_suite");
        if (cipherSuite != null) {
            out.println("<tr><td>");
            out.println("SSLCipherSuite:");
            out.println("</td><td>");
            out.println(HTMLFilter.filter(cipherSuite));
            out.println("</td></tr>");
        }
        out.println("</table>");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }
}
