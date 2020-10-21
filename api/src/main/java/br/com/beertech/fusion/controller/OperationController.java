package br.com.beertech.fusion.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.beertech.fusion.controller.dto.OperationDTO;
import br.com.beertech.fusion.controller.dto.TransferDTO;
import br.com.beertech.fusion.domain.Balance;
import br.com.beertech.fusion.domain.Operation;
import br.com.beertech.fusion.exception.FusionException;
import br.com.beertech.fusion.service.OperationService;
import br.com.beertech.fusion.service.PublishTransaction;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/bankbeer")
public class OperationController {

    @Autowired
    private OperationService operationService;

    @Autowired
    private PublishTransaction publisheTransaction;

  @GetMapping("/bank-statement/{hash}")
  public ResponseEntity<List<Operation>> listExtract(@PathVariable String hash) {
    List<Operation> transacoes = operationService.listTransaction(hash);
    return new ResponseEntity<>(transacoes, OK);
  }
    
  @GetMapping("/balance/{hash}")
  @PreAuthorize("hasRole('MODERATOR') or hasRole('USER')")
  public ResponseEntity<Balance> listBalanceAccount(@PathVariable String hash) {
    Balance saldo = operationService.calculateBalance(hash);
    return new ResponseEntity<>(saldo, OK);
  }

  @ApiIgnore
    @PostMapping("/operation/save")
    @PreAuthorize("hasRole('MODERATOR')")
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

  @ApiIgnore
    @PostMapping("/transfer/save")
    @PreAuthorize("hasRole('MODERATOR')")
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

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('ROLE_MODERATOR') or hasRole('ROLE_USER')")
    public CompletableFuture<ResponseEntity> queueTransfer(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody TransferDTO transferDTO) throws ExecutionException, InterruptedException {

        CompletableFuture<ResponseEntity> future = new CompletableFuture<>();
        try
        {
            // Run a task specified by a Supplier object asynchronously
            future = CompletableFuture.supplyAsync(new Supplier<ResponseEntity>() {
                @Override
                public ResponseEntity get()
                {
                    transferDTO.setAuthToken(token);
                	publisheTransaction.publisheTransfer(transferDTO);
                    return ResponseEntity.status(OK).body("Solicitação de Transferêcia executada!");
                }
            });
        }
        catch (Exception e)
        { e.printStackTrace(); }
        return CompletableFuture.completedFuture(future.get());
    }

}
