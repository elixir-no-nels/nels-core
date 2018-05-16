'''
Created on Jan 20, 2016

@author: Kidane
'''

from nels.storage.config import storage_config
from nels.storage import base_service

from flask import Flask, jsonify, abort, make_response
from flask.ext.httpauth import HTTPBasicAuth

app = Flask(__name__)
auth = HTTPBasicAuth()

@auth.get_password
def get_password(username):
    #print ("login requested by: %s" %username )
    if username in storage_config.Authorized_Clients.keys():
        return storage_config.Authorized_Clients[username]
    return None

@auth.error_handler
def unauthorized():
    return make_response(jsonify({'error': 'Unauthorized access'}), 401)

@app.route("/")
def index():
    return jsonify({'version':'2.1'})

@app.route('/users/<int:nels_id>', methods=['GET'])
@auth.login_required
def get_ssh_credential(nels_id):
    resp = base_service.get_ssh_credential(nels_id)
    if(resp == None):
        abort(404)
    if(resp['ssh_key'] == ''):
        abort(404)
        
    return jsonify({'hostname':resp['ssh_host'],'username':resp['user_name'],'key-rsa':resp['ssh_key']})

@app.route('/users/<int:nels_id>/base-directory', methods=['GET'])
@auth.login_required
def get_user_base_dir(nels_id):
    resp = base_service.get_ssh_credential(nels_id)
    if(resp == None):
        abort(404)
        if(resp['user_name'] == ''):
            abort(404)
    return jsonify({'username':resp['user_name'], 'base-directory':'/elixir-chr/nels/users/%s' %resp['user_name']})

    
def main():
    app.run(host=storage_config.HOST, port=storage_config.PORT)

if __name__ == "__main__":
    main()
