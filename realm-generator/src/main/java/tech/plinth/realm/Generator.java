package tech.plinth.realm;


import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.UUID;

public class Generator {
    public static void main(String[] args) throws Exception {
        Generator gen = new Generator();
        gen.parseArgs(args);
        gen.readTemplate();
        gen.transformRealm();
        gen.printRealm();

    }

    private CommandLine cmd;
    private JSONObject realm;
    private String originalId;
    private String newId;


    public void parseArgs(String[] args) {
        Options opts = new Options();
        opts.addOption("t", "template", true, "The template file to read (previous export)")
                .addOption("h", "help", false, "Print this help message")
                .addOption(Option.builder("n").longOpt("name").desc("The id of the new realm").hasArg().required().build())
                .addOption(Option.builder("d").longOpt("description").desc("The description of the new realm").hasArg().required().build());

        CommandLineParser parser = new DefaultParser();
        try {
            cmd = parser.parse(opts, args);
        } catch (org.apache.commons.cli.ParseException ex) {
            System.err.println("Could not execute: " + ex.getMessage());
            printHelp(opts);
            System.exit(-1);
        }

        if (cmd.hasOption("h")) {
            printHelp(opts);
            System.exit(0);
        }

    }

    public void transformRealm() {
        var newId = cmd.getOptionValue("n");

        originalId = realm.getString("id");
        this.newId = newId;

        realm.put("id", newId);
        realm.put("displayName", newId);
        realm.put("realm", newId);

        transformRoles(newId);

        transformGroups();

        transformScopes();

        transformAuthenticationFlows();

        transformClients();

        transformComponents();

    }

    public void printRealm() {
        System.out.println(realm.toString(2));
    }

    private void transformComponents() {
        JSONObject components = realm.getJSONObject("components");

        JSONArray clientRegistrationPolicy = components.getJSONArray("org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy");
        clientRegistrationPolicy.forEach(policy ->
                ((JSONObject) policy).put("id", UUID.randomUUID())
        );
        JSONArray keyProviders = components.getJSONArray("org.keycloak.keys.KeyProvider");
        keyProviders.forEach(provider -> ((JSONObject) provider).put("id", UUID.randomUUID()));
    }

    private void transformClients() {
        JSONArray clients = realm.getJSONArray("clients");
        clients.forEach(client -> {
            ((JSONObject) client).put("id", UUID.randomUUID());

            if (((JSONObject) client).has("secret")) {
                ((JSONObject) client).put("secret", UUID.randomUUID());
            }

            try {
                // modify the base url and the redirects for the default clients
                String baseUrl = ((JSONObject) client).getString("baseUrl");
                if (baseUrl != null && (baseUrl.startsWith("/auth/realms/") || baseUrl.startsWith("/auth/admin/"))) {
                    ((JSONObject) client).put("baseUrl", baseUrl.replace(originalId, newId));
                }
            } catch (JSONException ex) {
                // noop, not found
            }

            try {
                // modify the redirect urls of the default clients
                JSONArray redirects = ((JSONObject) client).getJSONArray("redirectUris");
                for (int i = 0; i < redirects.length(); i++) {
                    String uri = redirects.getString(i);
                    if (uri.startsWith("/auth/realms/") || uri.startsWith("/auth/admin/")) {
                        redirects.put(i, uri.replace(originalId, newId));
                    }
                }
            } catch (JSONException ex) {

            }

            try {
                // modify the protocol mappers for each client
                JSONArray protMappers = ((JSONObject) client).getJSONArray("protocolMappers");
                protMappers.forEach(mapper ->
                        ((JSONObject) mapper).put("id", UUID.randomUUID()));
            } catch (JSONException ex) {
                // noop, not found!
            }
        });
    }

    private void transformRoles(String newId) {
        JSONObject roles = realm.getJSONObject("roles");

        JSONArray realmRoles = roles.getJSONArray("realm");
        handleRoleArray(newId, realmRoles);

        JSONObject clients = roles.getJSONObject("client");
        clients.keySet().forEach(clientName ->
                handleRoleArray(newId, clients.getJSONArray(clientName))
        );
    }

    private void transformGroups() {
        JSONArray groups = realm.getJSONArray("groups");
        groups.forEach(group ->
                ((JSONObject) group).put("id", UUID.randomUUID())
        );
    }


    private void transformAuthenticationFlows() {
        JSONArray flows = realm.getJSONArray("authenticationFlows");
        flows.forEach(flow ->
                ((JSONObject) flow).put("id", UUID.randomUUID())
        );
    }

    private void transformScopes() {
        JSONArray groups = realm.getJSONArray("clientScopes");
        groups.forEach(group -> {
            ((JSONObject) group).put("id", UUID.randomUUID());
            try {
                var mappers = ((JSONObject) group).getJSONArray("protocolMappers");
                mappers.forEach(mapper ->
                        ((JSONObject) mapper).put("id", UUID.randomUUID()));
            } catch (JSONException ex) {
                // NOOP, not found
            }
        });
    }


    private void handleRoleArray(String newId, JSONArray realmRoles) {
        realmRoles.forEach(role -> {
            ((JSONObject) role).put("containerId", newId);
            ((JSONObject) role).put("id", UUID.randomUUID());
        });
    }


    private void printHelp(Options opts) {
        HelpFormatter formatter = new HelpFormatter();
        final PrintWriter writer = new PrintWriter(System.err);
        formatter.printHelp("Generator", opts);
        writer.flush();
    }

    private void readTemplate() throws IOException {
        Reader reader;
        if (cmd.hasOption("t")) {
            reader = new FileReader(cmd.getOptionValue("t"));

        } else {
            reader = new InputStreamReader(
                    this.getClass().getClassLoader()
                            .getResourceAsStream("realm-export.json"));
            System.err.println("You are creating a new realm out of the example export, this may overwrite " +
                    "your master realm if you import it. You have been warned");
        }
        StringWriter writer = new StringWriter();
        reader.transferTo(writer);
        realm = new JSONObject(writer.toString());
        reader.close();
        writer.close();
    }


}
