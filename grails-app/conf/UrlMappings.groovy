class UrlMappings {

	static mappings = {

        "/oauth/$provider/callback"(controller: 'oauth', action: 'callback')
        "/oauth/$provider/authenticate"(controller: 'oauth', action: 'authenticate')

        "/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
