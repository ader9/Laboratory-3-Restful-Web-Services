package rest.addressbook;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.After;
import org.junit.Test;

import rest.addressbook.AddressBook;
import rest.addressbook.ApplicationConfig;
import rest.addressbook.Person;

/**
 * A simple test suite
 *
 */
public class AddressBookServiceTest {

	HttpServer server;

	@Test
	public void serviceIsAlive() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		launchServer(ab);

		// Request the address book
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request().get();
		assertEquals(200, response.getStatus());

		//We can verify that GET method is secure because we only obtain information
		//and we don't modify anything of the server, also is idempotent because we can do
		//the same GET all times we want, and we will always obtain the same result
		response = client.target("http://localhost:8282/contacts")
				.request().get();
		assertEquals(200, response.getStatus());

		assertEquals(0, response.readEntity(AddressBook.class).getPersonList()
				.size());

		//////////////////////////////////////////////////////////////////////
		// Verify that GET /contacts is well implemented by the service, i.e
		// test that it is safe and idempotent
		//////////////////////////////////////////////////////////////////////	
	}

	@Test
	public void createUser() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		launchServer(ab);

		// Prepare data
		Person juan = new Person();
		juan.setName("Juan");
		URI juanURI = URI.create("http://localhost:8282/contacts/person/1");

		// Create a new user
		Client client = ClientBuilder.newClient();

		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));

		assertEquals(201, response.getStatus());
		assertEquals(juanURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Check that the new user exists
		response = client.target("http://localhost:8282/contacts/person/1")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		//////////////////////////////////////////////////////////////////////
		// Verify that POST /contacts is well implemented by the service, i.e
		// test that it is not safe and not idempotent
		//////////////////////////////////////////////////////////////////////

		//We can test that POST is not secure because we modify the server (adding a person) and
		//also is not idempotent because each time we do a POST we can see that is created in another
		//register
		URI juanURI2 = URI.create("http://localhost:8282/contacts/person/2");
		Response resp = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));
		assertEquals(juanURI2, resp.getLocation());

	}

	@Test
	public void createUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(ab.nextId());
		ab.getPersonList().add(salvador);
		launchServer(ab);

		// Prepare data
		Person juan = new Person();
		juan.setName("Juan");
		URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
		Person maria = new Person();
		maria.setName("Maria");
		URI mariaURI = URI.create("http://localhost:8282/contacts/person/3");

		// Create a user
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));
		assertEquals(201, response.getStatus());
		assertEquals(juanURI, response.getLocation());

		// Create a second user
		response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(201, response.getStatus());
		assertEquals(mariaURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person mariaUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaUpdated.getName());
		assertEquals(3, mariaUpdated.getId());
		assertEquals(mariaURI, mariaUpdated.getHref());

		// Check that the new user exists
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());


		//////////////////////////////////////////////////////////////////////
		// Verify that GET /contacts/person/3 is well implemented by the service, i.e
		// test that it is safe and idempotent
		//////////////////////////////////////////////////////////////////////

		//We can verify that GET method is secure because we only obtain information
		//and we don't modify anything of the server, also is idempotent because we can do
		//the same GET all times we want, and we will always obtain the same result.
		//In this case, the first response (when we do GET) is identically the same that the first one
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());

		mariaUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaUpdated.getName());
		assertEquals(3, mariaUpdated.getId());
		assertEquals(mariaURI, mariaUpdated.getHref());


	
	}

	@Test
	public void listUsers() throws IOException {

		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		Person juan = new Person();
		juan.setName("Juan");
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Test list of contacts
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		AddressBook addressBookRetrieved = response
				.readEntity(AddressBook.class);
		assertEquals(2, addressBookRetrieved.getPersonList().size());
		assertEquals(juan.getName(), addressBookRetrieved.getPersonList()
				.get(1).getName());

		//////////////////////////////////////////////////////////////////////
		// Verify that POST is well implemented by the service, i.e
		// test that it is not safe and not idempotent
		//////////////////////////////////////////////////////////////////////
		//We can test that POST is not secure because we modify the server (adding a person) and
		//also is not idempotent because each time we do a POST we can see that is created in another
		//register. In this case I compare the name of pepito, first in the register 2, and after do
		//the second test, in the register 3.


		Person pepito = new Person();
		pepito.setName("Pepito");
		URI pepitoURI = URI.create("http://localhost:8282/contacts/person/1");

		client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(pepito, MediaType.APPLICATION_JSON));

		assertEquals(pepito.getName(), ab.getPersonList()
				.get(2).getName());


		client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(pepito, MediaType.APPLICATION_JSON));

		assertEquals(pepito.getName(), ab.getPersonList()
				.get(3).getName());
	
	}

	@Test
	public void updateUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(ab.nextId());
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(ab.getNextId());
		URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Update Maria
		Person maria = new Person();
		maria.setName("Maria");
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), juanUpdated.getName());
		assertEquals(2, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		//////////////////////////////////////////////////////////////////////
		// Verify that PUT /contacts/person/2 is well implemented by the service, i.e
		// test that it is idempotent
		//////////////////////////////////////////////////////////////////////

		//We can check that PUT is not secure because we modified the server (when we add a person),
		//but it is idempotent, because we can check that each time we do a put and the resource is already created
		//(the person exist), this method doesn't create another person in the server
		response = client
				.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		juanUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), juanUpdated.getName());
		assertEquals(2, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Verify that the update is real
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person mariaRetrieved = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaRetrieved.getName());
		assertEquals(2, mariaRetrieved.getId());
		assertEquals(juanURI, mariaRetrieved.getHref());

		// Verify that only can be updated existing values
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(400, response.getStatus());


	
	}

	@Test
	public void deleteUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(1);
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(2);
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Delete a user
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/2").request()
				.delete();
		assertEquals(204, response.getStatus());

		// Verify that the user has been deleted
		response = client.target("http://localhost:8282/contacts/person/2")
				.request().delete();
		assertEquals(404, response.getStatus());


		//////////////////////////////////////////////////////////////////////
		// Verify that DELETE /contacts/person/2 is well implemented by the service, i.e
		// test that it is idempotent
		//////////////////////////////////////////////////////////////////////

		//We can check that DELETE is not secure because we modified the server (we delete the resource),
		// but it is idempotent because each time we execute DELETE the result is the same, the service without
		//the recurse. In this case we can check that the first time we delete the person Juan,
		//and we receive the code 204 which indicates that the server has successfully fulfilled the request and
		//the second time, if we execute the same, we received the code 404 (Not Found) which it means that the
		//resource is not available so DELETE is idempotent

		// Verify that the user has been deleted
		response = client.target("http://localhost:8282/contacts/person/2")
				.request().delete();
		assertEquals(404, response.getStatus());
	}

	@Test
	public void findUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(1);
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(2);
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Test user 1 exists
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/1")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person person = response.readEntity(Person.class);
		assertEquals(person.getName(), salvador.getName());
		assertEquals(person.getId(), salvador.getId());
		assertEquals(person.getHref(), salvador.getHref());

		// Test user 2 exists
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		person = response.readEntity(Person.class);
		assertEquals(person.getName(), juan.getName());
		assertEquals(2, juan.getId());
		assertEquals(person.getHref(), juan.getHref());

		// Test user 3 exists
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(404, response.getStatus());
	}

	private void launchServer(AddressBook ab) throws IOException {
		URI uri = UriBuilder.fromUri("http://localhost/").port(8282).build();
		server = GrizzlyHttpServerFactory.createHttpServer(uri,
				new ApplicationConfig(ab));
		server.start();
	}

	@After
	public void shutdown() {
		if (server != null) {
			server.shutdownNow();
		}
		server = null;
	}

}
