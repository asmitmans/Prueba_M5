package cl.fullstackjava.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cl.fullstackjava.model.dto.Address;
import cl.fullstackjava.model.dto.User;
import cl.fullstackjava.model.service.AddressService;
import cl.fullstackjava.model.service.Response;
import cl.fullstackjava.model.service.UserService;


@WebServlet("/user")
public class UserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private UserService userService = new UserService();
	private AddressService addressService = new AddressService();
	
	public UserServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Caché
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		
		String view = request.getParameter("view");
		String error = request.getParameter("error");
		HttpSession session = request.getSession(false);
		
		if (view == null) {
			RequestDispatcher dispatcher = request.getRequestDispatcher("/index.jsp");
			dispatcher.forward(request, response);
			return;
			
		} else if (view.equals("create")) {
			RequestDispatcher dispatcher = request.getRequestDispatcher("/views/registro.jsp");
			dispatcher.forward(request, response);
			return;
			
		} else if (view.equals("login")) {
			if (error != null) {
				request.setAttribute("error", error);
			}
			RequestDispatcher dispatcher = request.getRequestDispatcher("/views/login.jsp");
			dispatcher.forward(request, response);
			return;
			
		} else if (view.equals("home")) {
			
			if (session == null || session.getAttribute("username") == null) {
				response.sendRedirect(request.getContextPath() + "/user?view=login");
				return;
			}
			
			int userID = (int)session.getAttribute("userId");
			boolean hasPermission = userService.hasPermission(userID, "administrativo");
			
			if (!hasPermission) {
				session.setAttribute("message", "ERROR: Permisos insuficientes.");
				response.sendRedirect(request.getContextPath() + "/user?view=login");
				return;
			}
			
			List<Address> addresses = addressService.readWithUsers();
			request.setAttribute("addresses", addresses);
			
			request.getRequestDispatcher("/views/home.jsp").forward(request, response);
			return;
			
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String action = request.getParameter("action");
		
		try {
			if ("login".equals(action)) {
				login(request, response);
			} else if ("create".equals(action)) {
				create(request, response);
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Acción no válida");
				return;
			}
		} catch (IllegalArgumentException e) {
			request.setAttribute("error", e.getMessage());
			request.getRequestDispatcher("/error.jsp").forward(request, response);
			return;
		}
	}
	
	private void login(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String email = request.getParameter("email");
		String password = request.getParameter("password");
		HttpSession session = request.getSession();
		
		// Cache
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		
		boolean isValid = userService.login(email, password);
		
		if (isValid) {
			User user = userService.getUserByEmail(email);
			session.setAttribute("email", user.getEmail());
			session.setAttribute("username", user.getUsername());
			session.setAttribute("userId", user.getId());
			response.sendRedirect(request.getContextPath() + "/user?view=home");
			return;
		} else {
			session.setAttribute("message", "ERROR: Usuario o contraseña incorrectos");
			response.sendRedirect(request.getContextPath() + "/user?view=login");
			return;
		}
	}
	
	private void create(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String email = request.getParameter("email");
		String nick = request.getParameter("nick");
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		int weight = Integer.parseInt(request.getParameter("weight"));
		
		User user = new User();
		user.setEmail(email);
		user.setNick(nick);
		user.setUsername(username);
		user.setPassword(password);
		user.setWeight(weight);
		
		Response result = userService.create(user);
		
		HttpSession session = request.getSession();
		if (result.isSuccess()) {
			session.setAttribute("message", result.getMessage());
			response.sendRedirect(request.getContextPath() + "/");
			return;
		} else {
			session.setAttribute("message", "ERROR: " + result.getMessage());
			response.sendRedirect(request.getContextPath() + "/user?view=create");
			return;
		}
		
	}
	
	
}
