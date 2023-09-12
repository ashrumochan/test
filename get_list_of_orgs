from types import TracebackType
import requests
import json
from decouple import config
import datetime

url = '<URL GOES HERE>'
auth_token = "TOKEN GOES HERE"


def fetch_data(query, variables):
    headers = {
        'Authorization': f'bearer {auth_token}'
    }

    print(
        f"[*] Fetching data from \"{url}\" using \"{headers}\" with a query of:\n\t{query}\n\nVariables:\n\t{variables}")

    response = requests.post(url, json={'query': query, 'variables': variables}, headers=headers, verify=False)

    if response.status_code != 200:
        raise Exception(f'Request failed with status code {response.status_code}: {response.text}')

    data = json.loads(response.text)

    if 'errors' in data:
        raise Exception(f'Request failed with errors: {data}')

    return data['data']


def fetch_repositories(org_name, cursor):
    query = '''
    query ($orgName: String!, $repoCursor: String) {
        organization(login: $orgName) {
            name
            repositories(first: 100, after: $repoCursor) {
                edges {
                    node {
                        name
                        languages(first: 100) {
                            edges {
                                size
                                node {
                                    name
                                }
                            }
                            pageInfo {
                                endCursor
                                hasNextPage
                            }
                        }
                        repositories(first: 100) {
                            totalCount
                            pageInfo {
                                endCursor
                                hasNextPage
                            }
                        }
                    }
                }
                pageInfo {
                    endCursor
                    hasNextPage
                }
            }
        }
    }
    '''

    variables = {
        'orgName': org_name,
        'repoCursor': cursor,
    }

    data = fetch_data(query, variables)

    return data['organization']


def fetch_repo_languages(repo_name, cursor):
    query = '''
    query ($repoName: String!, $languageCursor: String) {
        repository(name: $repoName) {
            name
            languages(first: 100, after: $languageCursor) {
                edges {
                    size
                    node {
                        name
                    }
                }
                pageInfo {
                    endCursor
                    hasNextPage
                }
            }
        }
    }
    '''

    variables = {
        'repoName': repo_name,
        'languageCursor': cursor,
    }

    data = fetch_data(query, variables)
    return data['repository']


def fetch_organization(cursor):
    query = '''
        query ($orgCursor: String, $repoCursor: String, $languageCursor: String) {
        organizations(first: 10, after: $orgCursor) {
            edges {
            node {
                name
                repositories(first: 100, after: $repoCursor) {
                edges {
                    node {
                    name
                    languages(first: 100, after: $languageCursor) {
                        edges {
                        size
                        node {
                            name
                        }
                        }
                    }
                    }
                }
                pageInfo {
                    endCursor
                    hasNextPage
                }
                }
            }
            }
            pageInfo {
            endCursor
            hasNextPage
            }
        }
        }
        '''
    variables = {
        'orgCursor': cursor,
        'repoCursor': None,
        'languageCursor': None
    }

    data = fetch_data(query, variables)

    return data


def extract_edges(data):
    edges = []
    while True:
        for organization in data['organizations']['edges']:
            organization_name = organization['node']['name']
            print(f'[*] Processing organization: {organization_name}')
            repositories = organization['node']['repositories']
            repos = []
            while True:
                for repository in repositories['edges']:
                    repo_name = repository['node']['name']
                    languages = []
                    for language in repository['node']['languages']['edges']:
                        languages.append(language['node']['name'])
                    repos.append({"name": repo_name, "languages": languages})

                if not repositories['pageInfo']['hasNextPage']:
                    edges.append({organization_name: repos})
                    break
                repositories = fetch_repositories(organization_name, repositories['pageInfo']['endCursor'])

            print(f'[*] Processed organization: {organization_name} and it has {len(repos)} repositories')

        if not data['organizations']['pageInfo']['hasNextPage']:
            break

        data = fetch_organization(data['organizations']['pageInfo']["endCursor"])

    return edges


def main():
    data = fetch_organization(None)
    edges = extract_edges(data)
    print(edges)


if __name__ == "__main__":

    start_time = datetime.datetime.now()

    try:
        print(f"[*] START TIME: {start_time}")
        main()
    except KeyboardInterrupt:
        print(f"[*] Exiting...")
    except Exception as e:
        print(f"[-] ERROR: {e.with_traceback(None)}")
        exit(-1)
    finally:
        stop_time = datetime.datetime.now()
        print(f"[*] STOP TIME: {stop_time} ({stop_time - start_time} elapsed)")
