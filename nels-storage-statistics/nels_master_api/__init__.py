__import__('pkg_resources').declare_namespace(__name__)

CLIENT_KEY = ''
CLIENT_SECRET = ''
API_URL = ''


def get_full_url(relative_url):
    return API_URL + relative_url if API_URL.endswith("/") else "%s/%s" % (API_URL, relative_url)
