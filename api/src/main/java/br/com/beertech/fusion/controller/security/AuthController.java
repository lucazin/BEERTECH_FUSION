package br.com.beertech.fusion.controller.security;

import br.com.beertech.fusion.domain.CurrentAccount;
import br.com.beertech.fusion.domain.collections.CurrentAccountDocument;
import br.com.beertech.fusion.domain.security.request.LoginRequest;
import br.com.beertech.fusion.domain.security.request.SignupRequest;
import br.com.beertech.fusion.domain.security.response.JwtResponse;
import br.com.beertech.fusion.domain.security.response.MessageResponse;
import br.com.beertech.fusion.repository.RoleRepository;
import br.com.beertech.fusion.repository.UserRepository;
import br.com.beertech.fusion.domain.collections.UserDocument;
import br.com.beertech.fusion.service.CurrentAccountService;
import br.com.beertech.fusion.service.security.jwt.JwtUtils;
import br.com.beertech.fusion.service.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/bankbeer/auth")
public class AuthController {
	@Autowired
    AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	private CurrentAccountService currentAccountService;

	@Autowired
    PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok(new JwtResponse(jwt,
												 userDetails.getId(), 
												 userDetails.getUsername(), 
												 userDetails.getEmail(),
												 roles));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Erro: Usuário já existente"));
		}

		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Erro: E-mail já existente!"));
		}

		//Cria nova conta do cliente
		UserDocument usuario = new UserDocument(signUpRequest.getUsername(),
							 signUpRequest.getEmail(),
							 encoder.encode(signUpRequest.getPassword()),
							 signUpRequest.getCnpj(),signUpRequest.getNome());


		Set<String> strRoles = signUpRequest.getRole();

		usuario.setRoles(new ArrayList<>(strRoles));

		//Set<Role> roles = new HashSet<>();




//		// verifica roles
//		if (strRoles == null) {
//			Role userRole = roleRepository.findByName(EnumRole.ROLE_USER)
//					.orElseThrow(() -> new RuntimeException("Erro: Role não encontrada"));
//			roles.add(userRole);
//		} else {
//			strRoles.forEach(role -> {
//				switch (role) {
//				case "user":
//					Role adminRole = roleRepository.findByName(EnumRole.ROLE_USER)
//							.orElseThrow(() -> new RuntimeException("Erro: Role não encontrada"));
//					roles.add(adminRole);
//
//					break;
//				case "moderator":
//					Role modRole = roleRepository.findByName(EnumRole.ROLE_MODERATOR)
//							.orElseThrow(() -> new RuntimeException("Erro: Role não encontrada"));
//					roles.add(modRole);
//
//					break;
//				}
//			});
//		}
//
//
//		usuario.setRoles();
		userRepository.save(usuario);

		CurrentAccountDocument currentAccount = new CurrentAccountDocument();
		currentAccount.setCnpj(usuario.getCnpj());
		currentAccountService.saveAccount(currentAccount);

		return ResponseEntity.ok(new MessageResponse("Cliente cadastrado com sucesso!"));
	}
}
