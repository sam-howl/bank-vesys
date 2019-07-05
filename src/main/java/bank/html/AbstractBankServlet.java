package bank.html;

import javax.servlet.http.HttpServlet;

public abstract class AbstractBankServlet extends HttpServlet {
    public String getHeader(){
        return "<html><title>Welcome</title>";
    }

    public String getFooter(){
        return "<body><h1>Have a Great Day!</h1></body></html>";
    }
}
