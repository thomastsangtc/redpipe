package net.redpipe.example.helloworld;

import static net.redpipe.router.Router.getURI;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.annotations.Stream;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import net.redpipe.engine.template.Template;
import net.redpipe.fibers.Fibers;
import rx.Observable;
import rx.Single;

@Path("/")
public class HelloResource {
	@GET
	public String hello() {
		return "Hello World";
	}

	@Path("reactive")
	@GET
	public Single<String> helloReactive() {
		return Single.just("Hello Reactive World");
	}

	@Stream
	@Path("stream")
	@GET
	public Observable<String> helloStream() {
		return Observable.from(new String[] {"Hello", "World"});
	}

	@Path("composed")
	@GET
	public Single<String> helloComposed(@Context Vertx vertx) {
		Single<String> request1 = get(vertx, getURI(HelloResource::hello));
		Single<String> request2 = get(vertx, getURI(HelloResource::helloReactive));
			
		return request1.zipWith(request2, (hello1, hello2) -> hello1 + "\n" + hello2);
	}

	@Path("fiber")
	@GET
	public Single<String> helloFiber(@Context Vertx vertx,
			@Context UriInfo uriInfo) {
		return Fibers.fiber(() -> {
			String hello1 = Fibers.await(get(vertx, getURI(HelloResource::hello)));
			String hello2 = Fibers.await(get(vertx, getURI(HelloResource::helloReactive)));
			
			return hello1 + "\n" + hello2;
		});
	}

	private Single<String> get(Vertx vertx, URI uri){
		WebClient client = WebClient.create(vertx);
		Single<HttpResponse<Buffer>> responseHandler = 
				client.get(uri.getPort(), uri.getHost(), uri.getPath()).rxSend();

		return responseHandler.map(response -> response.body().toString());
	}
	
	@GET
	@Path("template")
	public Template template(){
		return new Template("templates/index.ftl")
				.set("title", "My page")
				.set("message", "Hello");
	}
}
