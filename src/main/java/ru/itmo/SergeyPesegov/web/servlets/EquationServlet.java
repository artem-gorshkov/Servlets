package ru.itmo.SergeyPesegov.web.servlets;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.zip.DataFormatException;

import static javax.servlet.http.HttpServletResponse.*;

@WebServlet(
        urlPatterns = {"/calc/*"}
)
public class EquationServlet extends HttpServlet {

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ServletContext context = getServletContext();
        String var = req.getPathInfo().replaceAll("/", "");
        System.err.println(var);
        if (context.getAttribute(var) == null) {
            context.setAttribute(var, "a+b/c");
            resp.setStatus(SC_CREATED);
            resp.setHeader("Location", req.getContextPath());
        } else {
            context.setAttribute(var, req.getParameter(var));
            resp.setStatus(SC_OK);
        }
        try {
        if (var.equals("equation")) {
                parseIntoPolishNotation(var);
        } else {
            int arg = Integer.parseInt(req.getParameter(var));
            if (arg < -10000 || arg > 10000)
                throw new DataFormatException();
        }
        } catch (ParseException e) {
            resp.sendError(SC_BAD_REQUEST, e.getMessage());
        } catch (NumberFormatException ee) {
            resp.sendError(SC_BAD_REQUEST, "Wrong number format");
        } catch (DataFormatException eee) {
            resp.sendError(SC_FORBIDDEN, "Exceeding values");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ServletContext context = getServletContext();
        String var = req.getPathInfo().replaceAll("/", "");;
        context.setAttribute(var, null);
        resp.setStatus(SC_NO_CONTENT);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ServletContext context = getServletContext();
        try {
            List<String> tokens = parseIntoPolishNotation(context.getAttribute("equation").toString());
            Map<String, Integer> args = new HashMap<>();
            Map<String, String> StringValue = new HashMap<>(); //for variable having  name of another variable
            Enumeration<String> names = context.getAttributeNames();
            while(names.hasMoreElements()) {
                String key = names.nextElement();
                String value = context.getAttribute(key).toString();
                if (key.equals("equation")) continue;
                try {
                    args.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    StringValue.put(key, value);
                }
            }
            for (String key : StringValue.keySet()) {
                Integer value = args.get(StringValue.get(key));
                args.put(key, value);
            }
            int result = calculate(tokens,args);
            resp.setStatus(SC_OK);
            resp.getOutputStream().print(result);
            resp.getOutputStream().flush();
            resp.getOutputStream().close();
        } catch (ParseException e) {
            resp.sendError(SC_CONFLICT, "Problem with equation");
        } catch (NumberFormatException ee) {
            resp.sendError(SC_CONFLICT, "Problem with variable");
        } catch (NullPointerException eee) {
            resp.sendError(SC_CONFLICT, "Not set required parameters");
            eee.printStackTrace();
            System.err.println(context.getAttribute("equation"));
            Enumeration<String> names = context.getAttributeNames();
            while(names.hasMoreElements()) {
                System.err.println(context.getAttribute(names.nextElement()));
            }
        }
    }

    private List<String> parseIntoPolishNotation(String equation) throws ParseException {
        List<String> tokens = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(equation.replaceAll(" ", ""), "()+-/*", true);
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }
        Deque<String> stack = new ArrayDeque<>();
        List<String> polishNotation = new ArrayList<>();
        for (String token : tokens) {
            try {
                switch (token) {
                    case "+":
                    case "-":
                    case ")":
                        while ("+-/*".contains(stack.getFirst())) {
                            polishNotation.add(stack.removeFirst());
                        }
                        if (token.equals(")")) {
                            if (!stack.getFirst().equals("("))
                                throw new ParseException("notValidEquation", 0);
                            stack.removeFirst();
                        } else {
                            stack.addFirst(token);
                        }
                        break;
                    case "*":
                    case "/":
                        while ("/*".contains(stack.getFirst())) {
                            polishNotation.add(stack.removeFirst());
                        }
                        stack.addFirst(token);
                        break;
                    case "(":
                        stack.addFirst(token);
                        break;
                    default:
                        polishNotation.add(token);
                }
            } catch (NoSuchElementException e) {
                if (!token.equals(")"))
                    stack.addFirst(token);
            }
        }
        while (!stack.isEmpty()) {
            if(stack.getFirst().equals("("))
                throw new ParseException("notValidEquation", 0);
            polishNotation.add(stack.removeFirst());
        }
        return polishNotation;
    }

    private static int calculate(List<String> polishNotation, Map<String, Integer> args) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (String token : polishNotation) {
            int first;
            int second;
            switch (token) {
                case "+":
                    stack.addFirst(stack.removeFirst() + stack.removeFirst());
                    break;
                case "-":
                    first = stack.removeFirst();
                    second = stack.removeFirst();
                    stack.addFirst(second - first);
                    break;
                case "*":
                    stack.addFirst(stack.removeFirst() * stack.removeFirst());
                    break;
                case "/":
                    first = stack.removeFirst();
                    second = stack.removeFirst();
                    stack.addFirst(second / first);
                    break;
                default:
                    if(args.containsKey(token)) {
//                        System.err.println(args.get(token));
                        stack.addFirst(args.get(token));
                    } else {
                        int arg = Integer.parseInt(token); //throw exception
                        stack.addFirst(arg);
                    }

            }
        }
        return stack.removeFirst();
    }
}
