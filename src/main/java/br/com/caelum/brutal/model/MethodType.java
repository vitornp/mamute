package br.com.caelum.brutal.model;

import br.com.caelum.brutal.infra.Digester;

public enum MethodType {
	BRUTAL {
		@Override
		public void updateForgottenPassword(String password,
				LoginMethod loginMethod) {
			loginMethod.setToken(Digester.encrypt(password));
		}

		@Override
		public void setPassword(LoginMethod loginMethod, String password) {
			loginMethod.setToken(Digester.encrypt(password));
			
		}
	}, 
	
	FACEBOOK {
		@Override
		public void updateForgottenPassword(String password,
				LoginMethod loginMethod) {
			//do nothing, really
		}

		@Override
		public void setPassword(LoginMethod loginMethod, String token) {
			loginMethod.setToken(token);
			
		}
	};

	public abstract void updateForgottenPassword(String password, LoginMethod loginMethod);

	public abstract void setPassword(LoginMethod loginMethod, String password);
}
