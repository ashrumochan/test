from types import TracebackType
import requests
import json
from decouple import config
import datetime

# Github Enterprise
url = 'https://github.aetna.com/api/graphql'
auth_token = config("PERSONAL_ACCESS_TOKEN")

# Github SaaS
# ENTERPRISE_SLUG = "cvs-health"
# url = f"https://api.github.com/enterprises/{ENTERPRISE_SLUG}/graphql"
# auth_token = config("GH_EMU_PERSONAL_ACCESS_TOKEN")

def fetch_data(query, variables):
    response = None
    headers = {
        'Authorization': f'bearer {auth_token}'
    }
    
    print(f"[*] Fetching data from \"{url}\" using \"{headers}\" with a query of:\n\t{query}\n\nVariables:\n\t{variables}")
    
    response = requests.post(url, json={'query': query, 'variables': variables}, headers=headers, verify=False)

    if response.status_code != 200:
        raise Exception(f'Request failed with status code {response.status_code}: {response.text}')

    data = json.loads(response.text)

    if 'errors' in data:
        raise Exception(f'Request failed with errors: {data}')

    return data['data']


def extract_edges(data, level):
    edges = []
    cursor = None

    if level == 'org':
        cursor_key = 'orgCursor'
        data_key = 'organizations'

    elif level == 'repo':
        cursor_key = 'repoCursor'
        data_key = 'repositories'

    elif level == 'language':
        cursor_key = 'languageCursor'
        data_key = 'languages'

    items = data[data_key]['edges']
    for item in items:
        edges.append(item['node'])
        if level == 'repo':
            repo_edges = []
            repository = item['node']
            repo_cursor = None
            while True:
                repo_items = repository['languages']['edges']
                for repo_item in repo_items:
                    repo_edges.append(repo_item['node'])
                if 'pageInfo' not in repository['languages']:
                    repo_page_info = {'hasNextPage': False, 'endCursor': None}
                else:
                    repo_page_info = repository['languages']['pageInfo']
                if not repo_page_info['hasNextPage']:
                    break
                repo_cursor = repo_page_info['endCursor']
                repository = fetch_repo_languages(repository['name'], repo_cursor)

                repository['languages']['edges'] = repo_edges

            repo_cursor = None
            while True:   # to extract all the repository cursors in its hierarchy level
                if 'repositories' in repository:
                    repo_page_info = repository['repositories']['pageInfo']
                else:
                    repo_page_info = {'hasNextPage': False, 'endCursor': None}
                if not repo_page_info['hasNextPage']:
                    break
                repo_cursor = repo_page_info['endCursor']

                ### Need to exhaust the inner-most cursor first
                if 'repositories' in repository:
                    repo_items = repository['repositories']['edges']
                    for repo_item in repo_items:
                        repo_node = repo_item['node']
                        repo_node_edges = []
                        while True:
                            repo_node_edges = repo_node_edges + repo_node['languages']['edges']
                            if 'pageInfo' not in repo_node['languages']:
                                repo_node_page_info = {'hasNextPage': False, 'endCursor': None}
                            else:
                                repo_node_page_info = repo_node['languages']['pageInfo']
                            if not repo_node_page_info['hasNextPage']:
                                break
                            repo_node_cursor = repo_node_page_info['endCursor']
                            repo_node = fetch_repo_languages(repo_node['name'], repo_node_cursor)
                        repo_node['languages']['edges'] = repo_node_edges

                    repository = fetch_repositories(repository['name'], repo_cursor)
                    repository['repositories']['edges'] = repo_items

            org_cursor = None
            while True:   # to extract all the org cursors in its hierarchy level
                org_items = item['node']['repositories']['edges']
                for org_item in org_items:
                    org_repo_edges = []
                    repository = org_item['node']
                    repo_cursor = None
                    while True:
                        repo_items = repository['languages']['edges']
                        for repo_item in repo_items:
                            org_repo_edges.append(repo_item['node'])
                        if 'pageInfo' not in repository['languages']:
                            repo_page_info = {'hasNextPage': False, 'endCursor': None}
                        else:
                            repo_page_info = repository['languages']['pageInfo']
                        if not repo_page_info['hasNextPage']:
                            break
                        repo_cursor = repo_page_info['endCursor']
                        repository = fetch_repo_languages(repository['name'], repo_cursor)
                    repository['languages']['edges'] = org_repo_edges

                org_page_info = item['node']['repositories']['pageInfo']
                if not org_page_info['hasNextPage']:
                    break
                org_cursor = org_page_info['endCursor']
                item['node']['repositories'] = fetch_repositories(item['node']['name'], org_cursor)['repositories']

        if data_key in data and 'pageInfo' in data[data_key]:
            page_info = data[data_key]['pageInfo']
            if page_info['hasNextPage']:
                cursor = page_info['endCursor']

    return edges, cursor_key, cursor

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
    # headers = {
    #     'Authorization': f'bearer {auth_token}',
    #     'Accept': 'application/vnd.github.v3+json'
    # }
    
    # response = requests.post(url, json={'query': query, 'variables': variables}, headers=headers, verify=False)
    
    # if response.status_code != 200:
    #     raise Exception(f'Request failed with status code {response.status_code}: {response.text}')
    
    # data = json.loads(response.text)
    
    # if 'errors' in data:
    #     raise Exception(f'Request failed with errors: {data}')
    
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
    # headers = {
    #     'Authorization': f'bearer {auth_token}',
    #     'Accept': 'application/vnd.github.v3+json'
    # }

    # response = requests.post(url, json={'query': query, 'variables': variables}, headers=headers, verify=False)

    # if response.status_code != 200:
    #     raise Exception(f'Request failed with status code {response.status_code}: {response.text}')

    # data = json.loads(response.text)

    # if 'errors' in data:
    #     raise Exception(f'Request failed with errors: {data}')

    return data['repository']

def main():
    org_cursor = None
    repo_cursor = None
    language_cursor = None

    all_edges = []

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
        'orgCursor': org_cursor,
        'repoCursor': repo_cursor,
        'languageCursor': language_cursor
    }

    headers = {
        'Authorization': f'bearer {auth_token}',
        'Accept': 'application/vnd.github.v3+json'
    }

    data = fetch_data(query, variables)
    
    org_edges, org_cursor_key, org_cursor = extract_edges(data, 'org')

    all_edges += org_edges

    start_time = datetime.datetime.now()

    while org_cursor is not None:
        variables[org_cursor_key] = org_cursor
        data = fetch_data(query, variables)
        org_edges, org_cursor_key, org_cursor = extract_edges(data, 'org')
        all_edges += org_edges

        processed_orgs = len(all_edges)
        processed_repos = sum(len(org['repositories']['edges']) for org in all_edges)
        print(f'[*] Processed {processed_orgs} organizations, {processed_repos} repositories.')
        
        current_time = datetime.datetime.now()
        time_per_org = (current_time - start_time) / processed_orgs
        remaining_orgs = 1300 - processed_orgs
        remaining_time = (remaining_orgs * time_per_org)
        expected_completion_time = datetime.datetime.now() + remaining_time

        print(f"    - Start....: {start_time} \tElapsed..........: {current_time - start_time}")
        print(f"    - End (est): {expected_completion_time} \tRemaining (est)..: {remaining_time}")

    print(f"--------------------------------------------------------------------------------")
    # print(f"[*] Writing output to \"results.json\"... ", end="")
    # with open('results.json', 'w') as f:
    #     json.dump(all_edges, f)
    # print(f"Done.")


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