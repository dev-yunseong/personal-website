#!/bin/sh
# curl | sh에서 함수 전체를 받은 뒤에만 설치를 시작한다.
website_install() {
    (
        set -eu

        install_dir=${WEBSITE_INSTALL_DIR:-${HOME:?HOME must be set}/.local/bin}
        command -v curl >/dev/null 2>&1 || {
            printf '%s\n' 'Error: curl is required.' >&2
            exit 1
        }
        mkdir -p "$install_dir"
        destination="$install_dir/website"
        if [ -d "$destination" ]; then
            printf '%s\n' "Error: $destination is a directory." >&2
            exit 1
        fi

        support_directory="$install_dir/.website"
        if [ -L "$support_directory" ] || { [ -e "$support_directory" ] && [ ! -d "$support_directory" ]; }; then
            printf '%s\n' "Error: $support_directory must be a directory, not a symbolic link or file." >&2
            exit 1
        fi
        temporary_directory=$(mktemp -d "$install_dir/.website-install.XXXXXX")
        temporary_file="$temporary_directory/website"
        installed=false
        support_replaced=false
        support_backed_up=false
        cleanup() {
            status=$?
            if [ "$installed" != true ]; then
                if [ "$support_replaced" = true ]; then
                    rm -rf "$support_directory"
                fi
                if [ "$support_backed_up" = true ]; then
                    mv "$temporary_directory/previous-support" "$support_directory"
                fi
            fi
            rm -rf "$temporary_directory"
            exit "$status"
        }
        trap cleanup 0
        trap 'exit 1' 1 2 3 15

        curl --fail --silent --show-error --location \
            --proto '=https' --proto-redir '=https' \
            --connect-timeout 10 --max-time 60 \
            https://yunseong.dev/cli/website --output "$temporary_file"
        mkdir "$temporary_directory/support"
        curl --fail --silent --show-error --location \
            --proto '=https' --proto-redir '=https' \
            --connect-timeout 10 --max-time 60 \
            https://yunseong.dev/cli/website-project-report/SKILL.md \
            --output "$temporary_directory/support/SKILL.md"
        curl --fail --silent --show-error --location \
            --proto '=https' --proto-redir '=https' \
            --connect-timeout 10 --max-time 60 \
            https://yunseong.dev/cli/project-report.json \
            --output "$temporary_directory/support/project-report.json"

        IFS= read -r first_line < "$temporary_file" || first_line=''
        if [ "$first_line" != '#!/bin/sh' ] || ! sh -n "$temporary_file"; then
            printf '%s\n' 'Error: downloaded website is not a valid shell command.' >&2
            exit 1
        fi
        chmod 755 "$temporary_file"
        if [ ! -s "$temporary_directory/support/SKILL.md" ] || [ ! -s "$temporary_directory/support/project-report.json" ]; then
            printf '%s\n' 'Error: downloaded skill files are empty.' >&2
            exit 1
        fi
        chmod 755 "$temporary_directory/support"
        chmod 644 "$temporary_directory/support/SKILL.md" "$temporary_directory/support/project-report.json"
        if [ -d "$support_directory" ]; then
            mv "$support_directory" "$temporary_directory/previous-support"
            support_backed_up=true
        fi
        mv "$temporary_directory/support" "$support_directory"
        support_replaced=true
        mv -f "$temporary_file" "$destination"
        installed=true
        printf 'Installed website to %s\n' "$destination"
        case ":${PATH:-}:" in
            *:"$install_dir":*) ;;
            *)
                printf 'Add this directory to PATH in your shell configuration: %s\n' "$install_dir"
                if [ -z "${WEBSITE_INSTALL_DIR:-}" ]; then
                    printf '%s\n' 'export PATH="$HOME/.local/bin:$PATH"'
                fi
                ;;
        esac
        printf '%s\n' 'Run website memo tree to get started. Run this installer again to update.'
    )
}
website_install
