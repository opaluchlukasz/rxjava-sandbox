package com.github.opaluchlukasz.rxjavasandbox;

import io.reactivex.processors.PublishProcessor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;

/**
 * Example implementation of simple banking system with central event bus implemented using PublishProcessor.
 *
 * Available operations:
 * * balance <accountNumber> -- checks account balance
 * * transfer <senderAccountNumber> <receiverAccountNumber> <amount> -- transfers amount from sender to receiver
 * * create <accountNumber> -- creates account
 */
public class SimpleBankingSystem {

    private static Map<String, Account> ACCOUNTS = new HashMap<>();

    public static void main(String[] args) {
        PublishProcessor<Command> processor = PublishProcessor.create();
        createAnAccount("123456", new BigDecimal("1300.00"), processor);
        createAnAccount("999999", new BigDecimal("400.00"), processor);
        createAnAccount("111111", new BigDecimal("1000.00"), processor);

        Scanner in = new Scanner(System.in);
        while(true) {
            String line = in.nextLine();
            if (line.equals("exit")) {
                break;
            }
            String[] command = line.split("\\s");
            if (command[0].equals("balance")) {
                String accountNumber = command[1];
                processor.onNext(Command.balance(accountNumber));
            }
            if (command[0].equals("transfer")) {
                String senderAccountNumber = command[1];
                String receiverAccountNumber = command[2];
                BigDecimal amount = new BigDecimal(command[3]);
                processor.onNext(Command.startTransfer(senderAccountNumber, receiverAccountNumber, amount));
            }
            if (command[0].equals("create")) {
                String accountNumber = command[1];
                if (ACCOUNTS.containsKey(accountNumber)) {
                    System.out.println("Account already exists");
                } else {
                    createAnAccount(accountNumber, ZERO, processor);
                }
            }
        }
    }

    private static class Account {
        private String accountNumber;
        private BigDecimal balance;
        private PublishProcessor<Command> processor;

        private Account(String accountNumber, BigDecimal balance, PublishProcessor<Command> processor) {
            this.accountNumber = accountNumber;
            this.balance = balance;
            this.processor = processor;
            processor
                    .filter(command -> accountNumber.equals(command.accountNumber))
                    .subscribe(command -> command.execute(this));
        }
    }

    private static class Command {
        private String accountNumber;
        private Consumer<Account> executeFn;

        private Command(String accountNumber, Consumer<Account> executeFn) {
            this.accountNumber = accountNumber;
            this.executeFn = executeFn;
        }

        private static Command startTransfer(String senderAccountNumber, String receiverAccountNumber, BigDecimal amount) {
            return new Command(senderAccountNumber, account -> {
                if (account.balance.compareTo(amount) >= 0) {
                    account.balance = account.balance.subtract(amount);
                    account.processor.onNext(finishTransfer(receiverAccountNumber, amount));
                } else {
                    System.out.println("Not enough funds");
                }
            });
        }

        private static Command finishTransfer(String receiverAccountNumber, BigDecimal amount) {
            return new Command(receiverAccountNumber, account -> account.balance = account.balance.add(amount));
        }

        private void execute(Account account) {
            executeFn.accept(account);
        }

        static Command balance(String accountNumber) {
            return new Command(accountNumber, account -> System.out.println(format("Account %s balance is %s",
                    account.accountNumber, account.balance.toPlainString())));
        }
    }

    private static void createAnAccount(String accountNumber, BigDecimal balance, PublishProcessor<Command> processor) {
        Account account = new Account(accountNumber, balance, processor);
        ACCOUNTS.put(accountNumber, account);
    }
}
