package com.rvbraga.sigec.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rvbraga.sigec.dto.ClienteDto;
import com.rvbraga.sigec.dto.MensagemDto;
import com.rvbraga.sigec.dto.PesquisaDto;
import com.rvbraga.sigec.dto.ProcessoDto;
import com.rvbraga.sigec.model.Cliente;
import com.rvbraga.sigec.model.Endereco;
import com.rvbraga.sigec.model.Processo;
import com.rvbraga.sigec.service.ClienteService;
import com.rvbraga.sigec.service.EnderecoService;
import com.rvbraga.sigec.service.ProcessoService;
import com.rvbraga.sigec.utils.Utilidades;

@Controller
@RequestMapping("/sigec") 
public class ClienteController {
	@Autowired
	private ClienteService clienteService; 
	@Autowired
	private ProcessoService processoService;
	@Autowired
	private EnderecoService enderecoService;
	@Autowired
	private Utilidades utilidades; 
	
	public static String UPLOAD_DIRECTORY = System.getProperty("user.home") + File.separator+"sigec"+File.separator+"digitalizacoes"+File.separator;

	@GetMapping("/login")
	public String login(Model model) {
		return "login.xhtml";
	}
	
	@GetMapping("/home")
	public String home() {
		return "home.html";  
	}
 
	@GetMapping("/clientes")
	public String cliente(Model model, @RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "5") int size) { 
		
		PesquisaDto pesquisa = new PesquisaDto();
		pesquisa.setPaginas(size);
		model.addAttribute("pesquisa", pesquisa); 
		model.addAttribute("texto_pagina", "Página ");
 
		try {
			List<Cliente> clientes = new ArrayList<Cliente>();
			Pageable paging = PageRequest.of(page - 1, size);

			Page<Cliente> pageClientes;
			pageClientes = clienteService.findAll(paging);

			clientes = pageClientes.getContent(); 		

			model.addAttribute("lista_clientes", clientes);
			model.addAttribute("currentPage", pageClientes.getNumber()+1);
			model.addAttribute("totalItems", pageClientes.getTotalElements());
			model.addAttribute("totalPages", pageClientes.getTotalPages());
			model.addAttribute("pageSize", size);
			model.addAttribute("mensagem_tabela", clientes.isEmpty() ? "Dados indisponíveis" : "");

		} catch (Exception e) {
			MensagemDto mensagem = new MensagemDto();
			mensagem.setMensagem(e.getMessage());
			mensagem.setStatus("Falha");
			model.addAttribute("feedback",mensagem);
			
			
			return "home.html";

		}
		return "cliente.html";
	}
 
	@PostMapping("/clientes/pesquisa") 
	public String pesquisaCliente(Model model, @Validated@ModelAttribute("pesquisa") PesquisaDto pesquisa,  Errors errors,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "5") int size) {
		
		List<Cliente> clientes = new ArrayList<Cliente>();
		if (errors.hasErrors()) { 
			model.addAttribute("texto_pagina", "Página ");
			model.addAttribute("lista_clientes", clientes);
			MensagemDto mensagem = new MensagemDto();
			mensagem.setMensagem("Corrija os campos assinalados!");
			mensagem.setStatus("AVISO");
			model.addAttribute("feedback",mensagem);
	        return "cliente_add_edit.html"; 
	    }
		model.addAttribute("texto_pagina", "Página ");
		if (!pesquisa.getPesquisaNome().isBlank())
			try {

				Pageable paging = PageRequest.of(page - 1, pesquisa.getPaginas());

				Page<Cliente> pageClientes;

				pageClientes = clienteService.findByNome(pesquisa.getPesquisaNome(), paging);
				clientes = pageClientes.getContent();
				model.addAttribute("lista_clientes", clientes);
				model.addAttribute("currentPage", pageClientes.getNumber() + 1);
				model.addAttribute("totalItems", pageClientes.getTotalElements());
				model.addAttribute("totalPages", pageClientes.getTotalPages());
				model.addAttribute("pageSize", size);
				model.addAttribute("mensagem_tabela", clientes.isEmpty() ? "Dados indisponíveis" : "");

			} catch (Exception e) {
				model.addAttribute("mensagem_tabela", e.getMessage());
				MensagemDto mensagem = new MensagemDto();
				mensagem.setMensagem(e.getMessage());
				mensagem.setStatus("FALHA");
				model.addAttribute("feedback",mensagem);
			}
		
		if (!pesquisa.getPesquisaCpf().isBlank()) {
			try {
				clientes.add(clienteService.findByCpf(pesquisa.getPesquisaCpf()));
				model.addAttribute("lista_clientes", clientes);
			}catch(Exception e) {
				
			}
		}
		if(pesquisa.getPesquisaCpf().isBlank()&&pesquisa.getPesquisaNome().isBlank()) {
			Pageable paging = PageRequest.of(page - 1, pesquisa.getPaginas());

			Page<Cliente> pageClientes;
			pageClientes = clienteService.findAll(paging);

			clientes = pageClientes.getContent(); 		

			model.addAttribute("lista_clientes", clientes);
			model.addAttribute("currentPage", pageClientes.getNumber()+1);
			model.addAttribute("totalItems", pageClientes.getTotalElements());
			model.addAttribute("totalPages", pageClientes.getTotalPages());
			model.addAttribute("pageSize", size);
			model.addAttribute("mensagem_tabela", clientes.isEmpty() ? "Dados indisponíveis" : "");
			
		}
		
		return "cliente.html";
	}

	@GetMapping("/cliente/adicionar")
	public String adicionarCliente(Model model) {
		
		Cliente cliente = new Cliente();	
		Endereco endereco = new Endereco();
		cliente.setEndereco(endereco);
		model.addAttribute("titulo_pagina", "Cadastro de clientes");		
		model.addAttribute("racas", utilidades.getRacas());
		model.addAttribute("generos", utilidades.getGeneros());
		model.addAttribute("cliente", cliente);
		
		return "cliente_add_edit.html";   
	}  

	@PostMapping("/cliente/salvar") 
	public String salvarCliente(Model model, @Validated Cliente cliente,@RequestParam("cpfdig") MultipartFile cpfDig, @RequestParam("rgdig") MultipartFile rgDig, @RequestParam("enddig") MultipartFile endDig, Errors errors, RedirectAttributes attributes)throws IOException {
		System.out.println("Entrou no controller salvar Cliente");
		File path; 
		
		path = new File(UPLOAD_DIRECTORY,cliente.getCpf().toString());
        path.mkdir();
		
        Path fileCpfDig =  Paths.get(UPLOAD_DIRECTORY,cliente.getCpf().toString() ,File.separator+cpfDig.getOriginalFilename());
        
        Path fileRgDig =  Paths.get(UPLOAD_DIRECTORY,cliente.getCpf().toString() ,File.separator+rgDig.getOriginalFilename());
        
        Path fileEndDig =  Paths.get(UPLOAD_DIRECTORY,cliente.getCpf().toString() ,File.separator+endDig.getOriginalFilename());
        
        
        cliente.setDataCadastro(LocalDate.now());
		cliente.setCpfDigital(fileCpfDig.toString());
		cliente.setRgDigital(fileRgDig.toString());
		cliente.setEnderecoDigital(fileEndDig.toString());
		
		System.out.println(UPLOAD_DIRECTORY);
		if (errors.hasErrors()) { 
			model.addAttribute("titulo_pagina", "Cadastro de clientes");			
			model.addAttribute("racas", utilidades.getRacas());
			model.addAttribute("generos", utilidades.getGeneros());
			model.addAttribute("cliente", cliente); 
			model.addAttribute("erros", errors);
			
			MensagemDto mensagem = new MensagemDto();
			mensagem.setMensagem("Corrija os campos assinalados");
			mensagem.setStatus("AVISO");
			model.addAttribute("feedback",mensagem);
			System.out.println("Encontrou erro -> voltando para a página");
			System.out.println(errors.toString()); 
	        return "cliente_add_edit.html"; 
	    }
		try {
			Endereco endereco_salvo = enderecoService.saveEndereco(cliente.getEndereco());
			cliente.setEndereco(endereco_salvo);
			Cliente cliente_salvo = clienteService.saveCliente(cliente);
			
	        Files.write(fileCpfDig, cpfDig.getBytes());
	        Files.write(fileCpfDig, endDig.getBytes());
	        Files.write(fileCpfDig, rgDig.getBytes());
			model.addAttribute("titulo_pagina", "Cadastro de clientes");
			MensagemDto mensagem = new MensagemDto();
			mensagem.setMensagem("Cliente " + cliente_salvo.getId() + " salvo!");
			mensagem.setStatus("SUCESSO");
			model.addAttribute("feedback",mensagem);
			System.out.println("Sucesso");
			
			//model.addAttribute("mensagem_sucesso", "Cliente " + cliente_salvo.getId() + " salvo!");
		} catch (Exception e) {
			model.addAttribute("titulo_pagina", "Cadastro de clientes");
			model.addAttribute("cliente", cliente);
			MensagemDto mensagem = new MensagemDto();
			mensagem.setMensagem(e.getMessage());
			mensagem.setStatus("FALHA");
			model.addAttribute("feedback",mensagem);
			System.out.println("Encontrou erro ao salvar -> voltando para a página");
			System.out.println(e.toString());
			return "cliente_add_edit.html";
		}
		return cliente(model,1,5);
	}
	 
	@GetMapping("/cliente/editar")  
	public String editarCliente(Model model, @ModelAttribute("id") UUID id) {
		
		ClienteDto cliente = new ClienteDto(clienteService.findById(id));
		
		model.addAttribute("titulo_pagina", "Edição de clientes");
		model.addAttribute("cliente",cliente);		
		model.addAttribute("generos", utilidades.getGeneros());
		model.addAttribute("racas", utilidades.getRacas());
		return "cliente_add_edit.html";
		
	}
	@PostMapping("/cliente/editar")
	public String salvarEdicao(Model model, @Validated ClienteDto cliente, Errors errors, RedirectAttributes attributes) {
		
		
		if (errors.hasErrors()) { 
			model.addAttribute("titulo_pagina", "Cadastro de clientes");
			model.addAttribute("campo_raca", "Raça:");
			model.addAttribute("racas", utilidades.getRacas());
			model.addAttribute("cliente", cliente); 
			model.addAttribute("erros", errors);
	        return "cliente_add_edit.html"; 
	    }
		Cliente cliente_editado = clienteService.saveCliente(clienteService.clienteDto2Cliente(cliente));
		MensagemDto mensagem = new MensagemDto();
		mensagem.setMensagem("Cliente " + cliente_editado.getId() + " editado!");
		mensagem.setStatus("SUCESSO");
		model.addAttribute("feedback",mensagem);
		
		return cliente(model,1,5);
	}
	
	@GetMapping("/cliente/deletar")
	public String deletarCliente(Model model, @ModelAttribute("id") UUID id) {
		
		clienteService.deleteCliente(id);
		MensagemDto mensagem = new MensagemDto();
		mensagem.setMensagem("Cliente deletado!");
		mensagem.setStatus("FALHA");
		model.addAttribute("feedback",mensagem);
		return cliente(model,1,5);
		
	}
	
	@GetMapping("/cliente/processo")
	public String processosCliente(Model model, @ModelAttribute("id") UUID id) {
		ProcessoDto processo = new ProcessoDto();
		String nomeCliente = clienteService.findById(id).getNome();
		processo.setIdCliente(id);
		model.addAttribute("titulo_pagina", "Cadastro de Processo");
		model.addAttribute("nome_cliente",nomeCliente);
		model.addAttribute("lista_status", utilidades.getStatusProcessos());
		model.addAttribute("lista_tipos", utilidades.getTiposProcessos());
		model.addAttribute("processo", processo);
		
		return "processo_add_edit.html";
		
	}
	
	@GetMapping("/cliente/visualizar") 
	public String visualizarCliente(Model model, @ModelAttribute("id") UUID id) {
		
		Cliente cliente = clienteService.findById(id);		
		List<Processo> processos = processoService.findProcessosByCliente(cliente);
		model.addAttribute("cliente",cliente);
		model.addAttribute("processos",processos);
		return "visualizar_cliente.html";
		
	} 
	
	@GetMapping("/resumo")
	public String resumo() {
		return "resumo.html";  
	}
	
	@GetMapping("/pagamentos")
	public String pagamentos() {
		return "pagamentos.html";   
	}
	
	
	
	@GetMapping("/agenda")
	public String agenda(Model model,@RequestParam(defaultValue="0")int ano_atual,@RequestParam(defaultValue="0")int mes_atual ) {
		//ZoneId brazilZoneId = ZoneId.of("America/Sao_Paulo");		
		
		LocalDate dataAtual = (ano_atual==0||mes_atual==0)?LocalDate.now(): LocalDate.of(ano_atual, mes_atual, 1);
		LocalDate hoje = LocalDate.now();
		
		LocalDate primeiroDia = LocalDate.of(dataAtual.getYear(), dataAtual.getMonth(), 1);
		ArrayList<Integer> mes = utilidades.fillMonth(primeiroDia.getMonth().length(primeiroDia.isLeapYear()));				
		model.addAttribute("nome_mes", utilidades.translateMonth(dataAtual.getMonth()));
		model.addAttribute("mes", mes);
		model.addAttribute("dias_semana", utilidades.getDiasSemana());
		model.addAttribute("primeiro_dia_semana_do_mes",utilidades.translateDayOfWeek(primeiroDia.getDayOfWeek()));
		model.addAttribute("mes_atual",dataAtual.getMonth().getValue());
		model.addAttribute("ano_atual", dataAtual.getYear()); 
		model.addAttribute("hoje",hoje.getDayOfMonth());
		model.addAttribute("texto_reunioes","Reuniões");
		
		return "agenda.html";  
	}
	
	
	

}
