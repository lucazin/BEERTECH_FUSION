package br.com.beertech.fusion.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.beertech.fusion.controller.dto.OperationDTO;
import br.com.beertech.fusion.controller.dto.TransferDTO;
import br.com.beertech.fusion.domain.Balance;
import br.com.beertech.fusion.domain.Operation;
import br.com.beertech.fusion.exception.FusionException;
import br.com.beertech.fusion.service.OperationService;
import br.com.beertech.fusion.service.SaldoService;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/bankbeer")
public class OperationController {

    @Autowired
    private OperationService operationService;

    @Autowired
    private SaldoService saldoService;


    @GetMapping("/transacoes")
    public CompletableFuture<List<Operation>> listOperations() throws ExecutionException, InterruptedException {

        CompletableFuture<List<Operation>> future = new CompletableFuture<>();
        try
        {
            // Run a task specified by a Supplier object asynchronously
            future = CompletableFuture.supplyAsync(new Supplier<List<Operation>>() {
                @Override
                public List<Operation> get()
                {
                    return operationService.ListaTransacoes();
                }
            });
        }
        catch (Exception e)
        { e.printStackTrace(); }
        return CompletableFuture.completedFuture(future.get());
    }

    @GetMapping("/saldo")
    public CompletableFuture<ResponseEntity> listSaldo() throws ExecutionException, InterruptedException {

        CompletableFuture<ResponseEntity> future = new CompletableFuture<>();
        try
        {
            // Run a task specified by a Supplier object asynchronously
            future = CompletableFuture.supplyAsync(new Supplier<ResponseEntity>() {
                @Override
                public ResponseEntity get()
                {
                    List<Operation> transacoes = operationService.ListaTransacoes();
                    Balance Saldo = saldoService.calcularSaldo(
                            transacoes.stream().map(Operation::getOperacaoDto).collect(Collectors.toList()));
                    return new ResponseEntity<>(Saldo, OK);
                }
            });
        }
        catch (Exception e)
        { e.printStackTrace(); }
        return CompletableFuture.completedFuture(future.get());
    }

    @GetMapping("/saldo/{hash}")
    public CompletableFuture<ResponseEntity> listSaldoConta(@PathVariable String hash) throws ExecutionException, InterruptedException {

        CompletableFuture<ResponseEntity> future = new CompletableFuture<>();
        try
        {
            // Run a task specified by a Supplier object asynchronously
            future = CompletableFuture.supplyAsync(new Supplier<ResponseEntity>() {
                @Override
                public ResponseEntity get()
                {
                    Balance saldo = operationService.calculateBalance(hash);
                    return new ResponseEntity<>(saldo, OK);
                }
            });
        }
        catch (Exception e)
        { e.printStackTrace(); }
        return CompletableFuture.completedFuture(future.get());
    }

    @PostMapping("/operacao/salvar")
    public CompletableFuture<ResponseEntity> saveOperations(@RequestBody OperationDTO operationDTO) throws ExecutionException, InterruptedException {

        CompletableFuture<ResponseEntity> future = new CompletableFuture<>();
        try
        {
            // Run a task specified by a Supplier object asynchronously
            future = CompletableFuture.supplyAsync(new Supplier<ResponseEntity>() {
                @Override
                public ResponseEntity get()
                {
                    Operation operacao = new Operation(operationDTO);
                    return new ResponseEntity<>(operationService.newTransaction(operacao), CREATED);
                }
            });
        }
        catch (Exception e)
        { e.printStackTrace(); }
        return CompletableFuture.completedFuture(future.get());
    }


    @PostMapping("/operacao")
    public CompletableFuture<ResponseEntity> queueOperationsx(@RequestBody OperationDTO operationDTO) throws ExecutionException, InterruptedException {

        CompletableFuture<ResponseEntity> future = new CompletableFuture<>();
        try
        {
            // Run a task specified by a Supplier object asynchronously
            future = CompletableFuture.supplyAsync(new Supplier<ResponseEntity>() {
                @Override
                public ResponseEntity get()
                {
                    operationService.publisheOperation(operationDTO);
                    return ResponseEntity.status(OK).body("Solicitação de Operação executada!");
                }
            });
        }
        catch (Exception e)
        { e.printStackTrace(); }
        return CompletableFuture.completedFuture(future.get());
    }

    @PostMapping("/transferencia/salvar")
    public CompletableFuture<ResponseEntity> saveTransfer(@RequestBody TransferDTO transferDTO) throws ExecutionException, InterruptedException {

        CompletableFuture<ResponseEntity> future = new CompletableFuture<>();
        try
        {
            // Run a task specified by a Supplier object asynchronously
            future = CompletableFuture.supplyAsync(new Supplier<ResponseEntity>() {
                @Override
                public ResponseEntity get()
                {
                    ResponseEntity EntityResponse = new ResponseEntity("",NO_CONTENT);
                    try
                    { EntityResponse = new ResponseEntity<>(operationService.saveTransfer(transferDTO), CREATED); }
                    catch (FusionException e)
                    {e.printStackTrace(); }

                    return  EntityResponse;
                }
            });
        }
        catch (Exception e)
        { e.printStackTrace(); }
        return CompletableFuture.completedFuture(future.get());
    }

    @PostMapping("/transferencia")
    @PreAuthorize("hasRole('MODERATOR')")
    public CompletableFuture<ResponseEntity> queueTransfer(@RequestBody TransferDTO transferDTO) throws ExecutionException, InterruptedException {

        CompletableFuture<ResponseEntity> future = new CompletableFuture<>();
        try
        {
            // Run a task specified by a Supplier object asynchronously
            future = CompletableFuture.supplyAsync(new Supplier<ResponseEntity>() {
                @Override
                public ResponseEntity get()
                {
                    operationService.publisheTransfer(transferDTO);
                    return ResponseEntity.status(OK).body("Solicitação de Transferêcia executada!");
                }
            });
        }
        catch (Exception e)
        { e.printStackTrace(); }
        return CompletableFuture.completedFuture(future.get());
    }

}
