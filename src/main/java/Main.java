import java.util.Collection;
import java.util.List;

public class Main {

    private PiazzaController controller;

    public Main() {
        this.controller = new PiazzaController();
    }

    /**
     * Use Case 1: Log in
     */
    public void useCase1() {

        // Logger inn med brukernavn og passord
        this.controller.logInUser("bendik@gmail.com", "bendik");

        // Logger ut
        this.controller.logOutUser();
    }

    /**
     * Use Case 2: Lag en post/thread med 'eksamen' som folder og 'question' som tag
     */
    public void useCase2() {

        // Logger inn med brukernavn og passord
        this.controller.logInUser("bendik@gmail.com", "bendik");

        // Lager en post
        this.controller.makeThread("Hva er hemmeligheten for å få A på prosjektet? Er det morsommer poster?",
                "Hvordan få A på prosjektet?", "eksamen", "TDT4145", 2021,
                "question", false);

        // Logger ut
        this.controller.logOutUser();
    }

    /**
     * Use Case 3: En instruktør skal svare på den forrige posten
     */
    public void useCase3() {
        // Logger inn med brukernavn og passord som instruktør
        this.controller.logInUser("sveinogroger@gmail.com", "svein");

        // Svarer på posten i forrige usecase
        this.controller.makeReply(1, "Det stemmer!", "Ja", false);

        // Logger ut
        this.controller.logOutUser();
    }

    /**
     * Use Case 4: Skal søke etter en post som inneholder 'WAL'
     */
    public void useCase4() {
        // Logger inn med brukernavn og passord
        this.controller.logInUser("bendik@gmail.com", "bendik");

        // Søker etter poster som inneholder 'WAL'
        Collection<Integer> poster = this.controller.searchFor("WAL", "TDT4145", 2021);  // 'searchFor' vil returnere en liste med alle id-ene som passer søket
        System.out.println(poster);

        // Logger ut
        this.controller.logOutUser();
    }

    /**
     * Use Case 5: Skal hente ut statistikk over hvor mange poster brukerne har sett og hvor mange poster brukerne har
     * laget i emnet
     */
    public void useCase5() {
        // Logger inn mec brukernavn og passord som instruktør
        this.controller.logInUser("sveinogroger@gmail.com", "svein");

        // Henter ut statistikken
        List<PiazzaController.UserStatistics> statistikk = this.controller.getStatistics("TDT4145", 2021);
        System.out.println(statistikk);

        // Logger ut
        this.controller.logOutUser();
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.useCase1();
        main.useCase2();
        main.useCase3();
        main.useCase4();
        main.useCase5();
    }
}
