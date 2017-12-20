import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

public class Solution {
  public static void main(String[] args) {

    SimpleDB db = new SimpleDB();
    String input;
    String[] commands;

    Scanner sc = new Scanner(System.in);
    while (sc.hasNextLine()) {

      input = sc.nextLine();
      commands = input.split(" ");
      if (commands.length == 0) {
        continue;
      }

      switch (commands[0]) {

        case "SET":
          if (commands.length == 3) {
            System.out.println(input);
            db.set(commands[1], commands[2]);
          }
          break;

        case "UNSET":
          if (commands.length == 2) {
            System.out.println(input);
            db.unset(commands[1]);
          }
          break;

        case "GET":
          if(commands.length == 2) {
            String got;
            System.out.println(input);
            if ((got = db.get(commands[1])) != null) {
              System.out.printf("> %s\n", got);
            } else {
              System.out.printf("> NULL\n");
            }
          }
          break;

        case "NUMEQUALTO":
          if (commands.length == 2) {
            System.out.println(input);
            System.out.printf("> %d\n", db.numEqualTo(commands[1]));
          }
          break;

        case "BEGIN":
          System.out.println(input);
          db.addTransaction();
          break;

        case "ROLLBACK":
          System.out.println(input);
          if (db.rollBack() != true) {
            System.out.printf("> NO TRANSACTION\n");
          }
          break;

        case "COMMIT":
          System.out.println(input);
          if (db.commit() != true) {
            System.out.printf("> NO TRANSACTION\n");
          }
          break;

        case "END":
          System.out.println(input);
          sc.close();
          System.exit(0);
          break;

      }
    }
  }

  public static class SimpleDB {

    // Main database
    private Hashtable<String, String> db;
    // Maintains counts of values in database
    private Hashtable<String, Integer> count;
    // Maintains order of Transactions
    private Stack<Transaction> transactions;
    // Transaction which the database currently has focus of
    private Transaction currTransaction;

    boolean rollingBack;

    public SimpleDB() {
      this.db = new Hashtable<String, String>();
      this.count = new Hashtable<String, Integer>();
      this.transactions = new Stack<Transaction>();
    }

    public void addTransaction() {
      currTransaction = new Transaction();
      transactions.push(currTransaction);
    }

    public boolean commit() {
      if (transactions.isEmpty()) {
        return false;
      } else {
        transactions.clear();
        return true;
      }
    }

    public void set(String key, String value) {
      if (db.containsKey(key) == false) {
        saveStateOfChangedRow(CommandType.UNSET, key, null);
        addToCount(value);
      } else {
        String oldValue = db.get(key);
        saveStateOfChangedRow(CommandType.SET, key, oldValue);
        subtractFromCount(oldValue);
        addToCount(value);
      }
      db.put(key, value);
    }

    public void unset(String key) {
      String removedVal = db.remove(key);
      if (removedVal != null) {
        saveStateOfChangedRow(CommandType.SET, key, removedVal);
        subtractFromCount(removedVal);
      }
    }

    public String get(String key) {
      if (db.containsKey(key)) {
        return db.get(key);
      }
      return null;
    }

    public Integer numEqualTo(String value) {
      if (count.containsKey(value)) {
        return count.get(value);
      }
      return 0;
    }

    public boolean rollBack() {
      rollingBack = true;

      if (transactions.isEmpty()) {
        return false;
      } else {
        transactions.pop();
        Interaction currInteraction;

        // Revert interactions to get original transaction state
        Set<String> keys = currTransaction.getInteractions().keySet();
        for (String key : keys) {
          currInteraction = currTransaction.getInteraction(key);
          switch (currInteraction.getType()) {

          case SET:
            set(currInteraction.key, currInteraction.getValue());
            break;

          case UNSET:
            unset(currInteraction.getKey());
            break;
          }
        }
      }
      currTransaction.getInteractions().clear();

      // Set currentTransaction to the transaction that is now in focus
      if (!transactions.isEmpty()) {
        currTransaction = transactions.peek();
      } else {
        currTransaction = null;
      }

      rollingBack = false;
      return true;
    }

    // If viable and state not prev saved, save the command to reverse the current interaction
    // in the transaction block, in case of rollback
    private void saveStateOfChangedRow(CommandType type, String key, String value) {
      if (!rollingBack && currTransaction != null && !currTransaction.stateAlreadySaved(key)) {
        currTransaction.saveInteraction(type, key, value);
      }
    }

    private void addToCount(String value) {
      if (count.containsKey(value)) {
        count.put(value, count.get(value) + 1);
      } else {
        count.put(value, 1);
      }
    }

    private void subtractFromCount(String value) {
      if (count.get(value) - 1 == 0) {
        count.remove(value);
      } else {
        count.put(value, count.get(value) - 1);
      }
    }

    // Maintains changes to a transaction for use in rollback
    public static class Transaction {

      // table of interactions for reversing to the transactions original state
      private Hashtable<String, Interaction> interactions;

      public Transaction() {
        this.interactions = new Hashtable<String, Interaction>();
      }

      public Hashtable<String, Interaction> getInteractions() {
        return interactions;
      }

      public void saveInteraction(CommandType type, String key, String value) {
        interactions.put(key, new Interaction(type, key, value));
      }

      public Interaction getInteraction(String key) {
        return interactions.get(key);
      }

      // Checks if a db row's keyVal has already been saved for this transaction
      public boolean stateAlreadySaved(String key) {
        return interactions.containsKey(key) ? true : false;
      }

    }

    // Stores data of desired interactions used for reversing commands during a rollback
    public static class Interaction {
      private CommandType type;
      private String key;
      private String value;

      public Interaction(CommandType type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
      }

      public CommandType getType() {
        return type;
      }

      public String getKey() {
        return key;
      }

      public String getValue() {
        return value;
      }
    }

    enum CommandType {
      SET, UNSET
    }

  }
}
