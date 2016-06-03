package com.zenware.roadfs.api;

import com.google.gson.Gson;
import com.zenware.roadfs.interfaces.iProvideCallback;
import com.zenware.roadfs.interfaces.iResponse;
import com.zenware.roadfs.model.*;
import com.zenware.roadfs.model.Error;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Response;


public class CallbackDirector<T> implements Callback<T> {
	private iProvideCallback<T> iProvideCallback;
	public CallbackDirector(iProvideCallback<T> iProvideCallback)
	{
		this.iProvideCallback = iProvideCallback;
	}

	private iResponse<T> getSuccessResponse(T t) throws NullPointerException
	{
		if(t != null) {
			iResponse<T> response = new iResponse<T>() {
				T t;

				@Override
				public void setResponse(T response) {
					t = response;
				}

				@Override
				public T getResponse() {
					return t;
				}
			};
			response.setResponse(t);
			return response;
		}
		else {
			throw new NullPointerException("Body is not of the correct type");
		}
	}

	private iResponse<Error> getErrorResponse(Error error)
	{
		iResponse<Error> response = new iResponse<Error>() {
			Error error;
			@Override
			public void setResponse(Error response) {
				error = response;
			}

			@Override
			public Error getResponse() {
				return error;
			}
		};
		response.setResponse(error);
		return response;
	}

	@Override
	public void onResponse(Response<T> response) {
		if(response.isSuccess())
		{
			try{
				iProvideCallback.onSuccess(this.getSuccessResponse(response.body()));
			}
			catch(NullPointerException npe)
			{
				this.iProvideCallback.onFailure(npe);
			}
		}
		else {
			int code = response.code();
			if(code > 299 && code <= 399)
			{
//              range of 300's should already be redirected
			}
			else if(code > 399 && code <= 499)
			{
//                range of 400's client side correction needed
//                TODO: might not be an com.zenware.roadfs.model.Error object
				String errorBody = null;
				try {
					errorBody = new String(response.errorBody().bytes());
					String contentType = response.headers().get("Content-Type");
					if(contentType == null)
					{
						iProvideCallback.onFailure(new Throwable(errorBody));
					}
					else {
//                        TODO: need to make sure that the body is a json.
						Gson gson = new Gson();
						Error error = gson.fromJson(errorBody, Error.class);
						iProvideCallback.onFailure(getErrorResponse(error));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			else if(code > 499 && code <= 599)
			{
//                range of 500's server side correction needed
				ResponseBody responseBody = response.errorBody();
				String errorBody = responseBody.toString();
				iProvideCallback.onFailure(new Throwable(errorBody));
			}
		}
	}

	@Override
	public void onFailure(Throwable t) {
		this.iProvideCallback.onFailure(t);
	}
}
